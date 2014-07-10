package models;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/*
 * Copyright 2014 hbz NRW (http://www.hbz-nrw.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/**
 * @author Jan Schnasse, schnasse@hbz-nrw.de
 * 
 * @see <a
 *      href="http://digitalsanctum.com/2012/06/07/basic-authentication-in-the-play-framework-using-custom-action-annotation/http://digitalsanctum.com/2012/06/07/basic-authentication-in-the-play-framework-using-custom-action-annotation">digitalsanctum</a>
 *      and http://stackoverflow.com/a/4412867/1485527
 */
@SuppressWarnings("javadoc")
public class User {
    String role = null;

    public User authenticate(String username, String password) {
	try {
	    Properties properties = new Properties();
	    properties.put(Context.INITIAL_CONTEXT_FACTORY,
		    "com.sun.jndi.ldap.LdapCtxFactory");
	    properties.put(Context.PROVIDER_URL, "ldap://localhost:389");
	    properties.put(Context.REFERRAL, "ignore");
	    String dn = dnFromUser(username);
	    properties.put(Context.SECURITY_PRINCIPAL, dn);
	    properties.put(Context.SECURITY_CREDENTIALS, password);
	    /*
	     * the next call ends in an exception if credentials are not valid
	     */
	    @SuppressWarnings("unused")
	    InitialDirContext context = new InitialDirContext(properties);
	    String group = groupFromUser(username);
	    role = group;
	} catch (Exception e) {
	    return null;
	}
	return this;
    }

    private String dnFromUser(String username) throws NamingException {
	Properties props = new Properties();
	props.put(Context.INITIAL_CONTEXT_FACTORY,
		"com.sun.jndi.ldap.LdapCtxFactory");
	props.put(Context.PROVIDER_URL, "ldap://localhost:389");
	props.put(Context.REFERRAL, "ignore");
	InitialDirContext context = new InitialDirContext(props);
	SearchControls ctrls = new SearchControls();
	ctrls.setReturningAttributes(new String[] { "givenName", "sn",
		"gidNumber", "cn", "ou", "dc" });
	ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
	NamingEnumeration<SearchResult> answers = context.search(
		"dc=edoweb-rlp,dc=de", "(cn=" + username + ")", ctrls);
	SearchResult result = answers.next();
	return result.getNameInNamespace();
    }

    private String groupFromUser(String username) throws NamingException {
	Properties props = new Properties();
	props.put(Context.INITIAL_CONTEXT_FACTORY,
		"com.sun.jndi.ldap.LdapCtxFactory");
	props.put(Context.PROVIDER_URL, "ldap://localhost:389");
	props.put(Context.REFERRAL, "ignore");
	InitialDirContext context = new InitialDirContext(props);
	SearchControls ctrls = new SearchControls();
	ctrls.setReturningAttributes(new String[] { "givenName", "sn",
		"gidNumber", "cn", "ou", "dc" });
	ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
	NamingEnumeration<SearchResult> answers = context.search(
		"dc=edoweb-rlp,dc=de", "(cn=" + username + ")", ctrls);
	Attributes result = answers.next().getAttributes();
	String groupNumber = result.get("gidNumber").get().toString();
	ctrls.setReturningAttributes(new String[] { "cn" });
	ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
	answers = context.search("ou=groups,dc=edoweb-rlp,dc=de", "(gidNumber="
		+ groupNumber + ")", ctrls);
	result = answers.next().getAttributes();
	String groupName = result.get("cn").get().toString();
	return groupName;
    }

    public String getRole() {
	return role;
    }

    public void setRole(String role) {
	this.role = role;
    }

}
