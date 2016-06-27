/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.grizzly2.filter;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

/**
 *
 * @author xiaolie
 */
public class AuthorizationRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
//        String user = context.getHeaderString("client-user");
//        if (!"shenjitang".equalsIgnoreCase(user)) {
//            context.abortWith(Response
//                    .status(Response.Status.UNAUTHORIZED)
//                    .entity("User cannot access the resource.")
//                    .build());
//        } 
    }
    
}
