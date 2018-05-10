/*
 * DRACOON
 * Version 4.4.0 - built at: 2017-12-04 04:14:43, API server: https://demo.dracoon.com/api/v4
 *
 * OpenAPI spec version: 4.4.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package ch.cyberduck.core.sds.io.swagger.client.model;

import java.util.Objects;
import ch.cyberduck.core.sds.io.swagger.client.model.KeyValueEntry;
import ch.cyberduck.core.sds.io.swagger.client.model.Language;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * AuthInitResources
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-05-03T10:55:56.129+02:00")
public class AuthInitResources {
  @JsonProperty("languages")
  private List<Language> languages = new ArrayList<Language>();

  @JsonProperty("authTypes")
  private List<KeyValueEntry> authTypes = new ArrayList<KeyValueEntry>();

  public AuthInitResources languages(List<Language> languages) {
    this.languages = languages;
    return this;
  }

  public AuthInitResources addLanguagesItem(Language languagesItem) {
    this.languages.add(languagesItem);
    return this;
  }

   /**
   * Supported languages
   * @return languages
  **/
  @ApiModelProperty(required = true, value = "Supported languages")
  public List<Language> getLanguages() {
    return languages;
  }

  public void setLanguages(List<Language> languages) {
    this.languages = languages;
  }

  public AuthInitResources authTypes(List<KeyValueEntry> authTypes) {
    this.authTypes = authTypes;
    return this;
  }

  public AuthInitResources addAuthTypesItem(KeyValueEntry authTypesItem) {
    this.authTypes.add(authTypesItem);
    return this;
  }

   /**
   * Supported authentication types
   * @return authTypes
  **/
  @ApiModelProperty(required = true, value = "Supported authentication types")
  public List<KeyValueEntry> getAuthTypes() {
    return authTypes;
  }

  public void setAuthTypes(List<KeyValueEntry> authTypes) {
    this.authTypes = authTypes;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuthInitResources authInitResources = (AuthInitResources) o;
    return Objects.equals(this.languages, authInitResources.languages) &&
        Objects.equals(this.authTypes, authInitResources.authTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(languages, authTypes);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuthInitResources {\n");
    
    sb.append("    languages: ").append(toIndentedString(languages)).append("\n");
    sb.append("    authTypes: ").append(toIndentedString(authTypes)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
  
}
