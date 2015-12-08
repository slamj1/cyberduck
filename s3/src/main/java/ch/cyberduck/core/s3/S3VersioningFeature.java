package ch.cyberduck.core.s3;

/*
 * Copyright (c) 2002-2013 David Kocher. All rights reserved.
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to feedback@cyberduck.ch
 */

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.VersioningConfiguration;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.features.AclPermission;
import ch.cyberduck.core.features.Versioning;
import ch.cyberduck.core.preferences.PreferencesFactory;

import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.S3BucketVersioningStatus;
import org.jets3t.service.model.S3Object;

import java.util.Collections;
import java.util.Map;

/**
 * @version $Id$
 */
public class S3VersioningFeature implements Versioning {
    private static final Logger log = Logger.getLogger(S3VersioningFeature.class);

    private S3Session session;

    private PathContainerService containerService
            = new S3PathContainerService();

    private Map<Path, VersioningConfiguration> cache
            = Collections.synchronizedMap(new LRUMap(10));

    public S3VersioningFeature(final S3Session session) {
        this.session = session;
    }

    @Override
    public S3VersioningFeature withCache(final Map<Path, VersioningConfiguration> cache) {
        this.cache = cache;
        return this;
    }

    @Override
    public void setConfiguration(final Path container, final LoginCallback prompt, final VersioningConfiguration configuration) throws BackgroundException {
        try {
            final VersioningConfiguration current = this.getConfiguration(container);
            if(current.isMultifactor()) {
                // The bucket is already MFA protected.
                final Credentials factor = this.getToken(prompt);
                if(configuration.isEnabled()) {
                    if(current.isEnabled()) {
                        log.debug("Versioning already enabled for bucket " + container);
                    }
                    else {
                        // Enable versioning if not already active.
                        log.debug("Enable bucket versioning with MFA " + factor.getUsername() + " for " + container);
                        session.getClient().enableBucketVersioningWithMFA(container.getName(),
                                factor.getUsername(), factor.getPassword());
                    }
                }
                else {
                    log.debug("Suspend bucket versioning with MFA " + factor.getUsername() + " for " + container);
                    session.getClient().suspendBucketVersioningWithMFA(container.getName(),
                            factor.getUsername(), factor.getPassword());
                }
                if(configuration.isEnabled() && !configuration.isMultifactor()) {
                    log.debug(String.format("Disable MFA %s for %s", factor.getUsername(), container));
                    // User has choosen to disable MFA
                    final Credentials factor2 = this.getToken(prompt);
                    session.getClient().disableMFAForVersionedBucket(container.getName(),
                            factor2.getUsername(), factor2.getPassword());
                }
            }
            else {
                if(configuration.isEnabled()) {
                    if(configuration.isMultifactor()) {
                        final Credentials factor = this.getToken(prompt);
                        log.debug(String.format("Enable bucket versioning with MFA %s for %s", factor.getUsername(), container));
                        session.getClient().enableBucketVersioningWithMFA(container.getName(),
                                factor.getUsername(), factor.getPassword());
                    }
                    else {
                        if(current.isEnabled()) {
                            log.debug(String.format("Versioning already enabled for bucket %s", container));
                        }
                        else {
                            log.debug(String.format("Enable bucket versioning for %s", container));
                            session.getClient().enableBucketVersioning(container.getName());
                        }
                    }
                }
                else {
                    log.debug(String.format("Susped bucket versioning for %s", container));
                    session.getClient().suspendBucketVersioning(container.getName());
                }
            }
            cache.remove(container);
        }
        catch(ServiceException e) {
            throw new ServiceExceptionMappingService().map("Failure to write attributes of {0}", e);
        }
    }

    @Override
    public VersioningConfiguration getConfiguration(final Path container) throws BackgroundException {
        if(cache.containsKey(container)) {
            return cache.get(container);
        }
        try {
            final S3BucketVersioningStatus status
                    = session.getClient().getBucketVersioningStatus(container.getName());
            final VersioningConfiguration configuration = new VersioningConfiguration(status.isVersioningEnabled(),
                    status.isMultiFactorAuthDeleteRequired());
            cache.put(container, configuration);
            return configuration;
        }
        catch(ServiceException e) {
            try {
                throw new ServiceExceptionMappingService().map("Cannot read bucket versioning status", e);
            }
            catch(AccessDeniedException l) {
                log.warn(String.format("Missing permission to read versioning configuration for %s %s", container, e.getMessage()));
                return VersioningConfiguration.empty();
            }
            catch(InteroperabilityException i) {
                log.warn(String.format("Not supported to read versioning configuration for %s %s", container, e.getMessage()));
                return VersioningConfiguration.empty();
            }
        }
    }

    /**
     * Versioning support. Copy a previous version of the object into the same bucket.
     * The copied object becomes the latest version of that object and all object versions are preserved.
     */
    @Override
    public void revert(final Path file) throws BackgroundException {
        if(file.isFile()) {
            try {
                final S3Object destination = new S3Object(containerService.getKey(file));
                // Keep same storage class
                destination.setStorageClass(file.attributes().getStorageClass());
                // Keep encryption setting
                destination.setServerSideEncryptionAlgorithm(file.attributes().getEncryption());
                // Apply non standard ACL
                final S3AccessControlListFeature acl = (S3AccessControlListFeature) session.getFeature(AclPermission.class);
                destination.setAcl(acl.convert(acl.getPermission(file)));
                session.getClient().copyVersionedObject(file.attributes().getVersionId(),
                        containerService.getContainer(file).getName(), containerService.getKey(file), containerService.getContainer(file).getName(), destination, false);
            }
            catch(ServiceException e) {
                throw new ServiceExceptionMappingService().map("Cannot revert file", e, file);
            }
        }
    }

    /**
     * Prompt for MFA credentials
     *
     * @param controller Prompt controller
     * @return MFA one time authentication password.
     * @throws ch.cyberduck.core.exception.ConnectionCanceledException Prompt dismissed
     */
    @Override
    public Credentials getToken(final LoginCallback controller) throws ConnectionCanceledException {
        final Credentials credentials = new MultifactorCredentials();
        // Prompt for multi factor authentication credentials.
        controller.prompt(session.getHost(), credentials,
                LocaleFactory.localizedString("Provide additional login credentials", "Credentials"),
                LocaleFactory.localizedString("Multi-Factor Authentication", "S3"), new LoginOptions());

        PreferencesFactory.get().setProperty("s3.mfa.serialnumber", credentials.getUsername());
        return credentials;
    }
}