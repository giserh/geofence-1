/*
 *  Copyright (C) 2007 - 2013 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.geoserver.authentication.auth;

import it.geosolutions.geofence.services.RuleReaderService;
import it.geosolutions.geofence.services.dto.AuthUser;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authentication provider that delegates to GeoFence
 * @author ETj (etj at geo-solutions.it)
 */
public class GeofenceAuthenticationProvider extends GeoServerAuthenticationProvider {

    private RuleReaderService ruleReaderService;

    @Override
    public boolean supports(Class<? extends Object> authentication, HttpServletRequest request) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

    @Override
    public Authentication authenticate(Authentication authentication, HttpServletRequest request)
            throws AuthenticationException {

        UsernamePasswordAuthenticationToken  outTok = null;
        LOGGER.log(Level.FINE, "Auth request with {0}", authentication);

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken inTok =  (UsernamePasswordAuthenticationToken)authentication;

            AuthUser authUser = null;
            try {
                authUser = ruleReaderService.authorize(
                        inTok.getPrincipal().toString(),
                        inTok.getCredentials().toString());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in authenticating with GeoFence", e);
                throw new AuthenticationException("Error in GeoFence communication", e) {};
            }

            if(authUser != null) {
                LOGGER.log(Level.FINE, "User {0} authenticated: {1}", new Object[]{inTok.getPrincipal(), authUser});

                List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
                roles.addAll(inTok.getAuthorities());
                roles.add(GeoServerRole.AUTHENTICATED_ROLE);
                if(authUser.getRole() == AuthUser.Role.ADMIN)
                    roles.add(GeoServerRole.ADMIN_ROLE);

                outTok = new UsernamePasswordAuthenticationToken(
                    inTok.getPrincipal(), inTok.getCredentials(), roles);

            } else {
                LOGGER.log(Level.INFO, "User {0} NOT authenticated", inTok.getPrincipal());
            }

            return outTok;

        } else {
            return null;
        }
    }

    public void setRuleReaderService(RuleReaderService ruleReaderService) {
        this.ruleReaderService = ruleReaderService;
    }

}
