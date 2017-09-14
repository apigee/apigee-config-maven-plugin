/**
 * Copyright (C) 2016 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apigee.edge.config.utils;

public class ServerProfile {

	private String application; // application name
	private String org;
	private String credential_user;
	private String credential_pwd; //
	private String hostURL; // hostname & scheme e.g.,
							// https://api.enterprise.apigee.com
	private String environment; // prod or test
	private String api_version; // v2 or v1 in the server url
	private String bundle_zip_full_path;
	private String profileId; //Profile id as in parent pom
	private String options;
	
	private String tokenURL; // Mgmt API OAuth token endpoint
	private String mfaToken; // Mgmt API OAuth MFA - TOTP
	private String clientId; //Mgmt API OAuth Client Id (optional)
	private String clientSecret; //Mgmt API OAuth Client Secret (optional)
	private String bearerToken; //Mgmt API OAuth Token
	private String refreshToken; //Mgmt API OAuth Refresh Token
	private String authType; // Mgmt API Auth Type oauth|basic
	
	public String getHostURL() {
		return hostURL;
	}

	public void setHostURL(String hostURL) {
		this.hostURL = hostURL;
	}

	public String getTokenUrl() {
		return tokenURL;
	}

	public void setTokenUrl(String tokenURL) {
		this.tokenURL = tokenURL;
	}

	public String getMFAToken() {
		return mfaToken;
	}

	public void setMFAToken(String mfaToken) {
		this.mfaToken = mfaToken;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getBearerToken() {
		return bearerToken;
	}

	public void setBearerToken(String bearerToken) {
		this.bearerToken = bearerToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getAuthType() {
		return authType;
	}

	public void setAuthType(String authType) {
		this.authType = authType;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public String getCredential_user() {
		return credential_user;
	}

	public void setCredential_user(String credential_user) {
		this.credential_user = credential_user;
	}

	public String getCredential_pwd() {
		return credential_pwd;
	}

	public void setCredential_pwd(String credential_pwd) {
		this.credential_pwd = credential_pwd;
	}

	public String getHostUrl() {
		return hostURL;
	}

	public void setHostUrl(String host) {
		this.hostURL = host;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getApi_version() {
		return api_version;
	}

	public void setApi_version(String api_version) {
		this.api_version = api_version;
	}

	public String getBundle_zip_full_path() {
		return bundle_zip_full_path;
	}

	public void setBundle_zip_full_path(String bundle_zip_full_path) {
		this.bundle_zip_full_path = bundle_zip_full_path;
	}

	/**
	 * @return the profileid
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * @param id the id to set
	 */
	public void setProfileId(String id) {
		this.profileId = id;
	}

	/**
	 * @param options the options to set
	 */
	
	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

}
