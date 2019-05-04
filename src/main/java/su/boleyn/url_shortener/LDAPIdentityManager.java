package su.boleyn.url_shortener;

import java.security.Principal;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;

public class LDAPIdentityManager implements IdentityManager {
	private String ldapHost;
	private String ldapBase;
	private String ldapAdminUsername;
	private String ldapAdminPassword;

	public LDAPIdentityManager(String ldapHost, String ldapBase, String ldapAdminUsername, String ldapAdminPassword)
			throws NamingException {
		this.ldapHost = ldapHost;
		this.ldapBase = ldapBase;
		this.ldapAdminUsername = ldapAdminUsername;
		this.ldapAdminPassword = ldapAdminPassword;
		getLdapContext(ldapAdminUsername, ldapAdminPassword).close();
	}

	private LdapContext getLdapContext(String ldapUsername, String ldapPassword) throws NamingException {
		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, ldapHost);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, ldapUsername);
		env.put(Context.SECURITY_CREDENTIALS, ldapPassword);
		return new InitialLdapContext(env, null);
	}

	@Override
	public Account verify(Account account) {
		return account;
	}

	@Override
	public Account verify(String id, Credential credential) {
		Account account = getAccount(id);
		if (account != null && verifyCredential(account, credential)) {
			return account;
		} else {
			return null;
		}
	}

	@Override
	public Account verify(Credential credential) {
		return null;
	}

	private boolean verifyCredential(Account account, Credential credential) {
		if (credential instanceof PasswordCredential) {
			String password = new String(((PasswordCredential) credential).getPassword());
			String username = account.getPrincipal().getName();
			try {
				getLdapContext(username, password).close();
			} catch (Exception e) {
				return false;
			}
			return true;
		}
		return false;
	}

	@SuppressWarnings("serial")
	private Account getAccount(final String id) {
		LdapContext ctx = null;
		NamingEnumeration<SearchResult> results = null;
		try {
			String searchFilter = "(&(objectClass=posixAccount)(uid=" + id + "))";
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			ctx = getLdapContext(ldapAdminUsername, ldapAdminPassword);
			results = ctx.search(ldapBase, searchFilter, searchControls);
			if (results.hasMoreElements()) {
				SearchResult searchResult = (SearchResult) results.nextElement();
				if (results.hasMoreElements()) {
					return null;
				}
				String username = searchResult.getNameInNamespace();
				return new Account() {
					private final Principal principal = new Principal() {
						@Override
						public String getName() {
							return username;
						}
					};

					@Override
					public Principal getPrincipal() {
						return principal;
					}

					@Override
					public Set<String> getRoles() {
						return Collections.emptySet();
					}
				};
			}
		} catch (NamingException e) {
		} finally {
			try {
				if (results != null) {
					results.close();
				}
			} catch (NamingException e) {
			}
			try {
				if (ctx != null) {
					ctx.close();
				}
			} catch (NamingException e) {
			}
		}
		return null;
	}
}
