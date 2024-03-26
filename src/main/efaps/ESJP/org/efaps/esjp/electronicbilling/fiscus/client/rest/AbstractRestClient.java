/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.efaps.esjp.electronicbilling.fiscus.client.rest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.electronicbilling.fiscus.client.dto.SSOLoginResponseDto;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.util.EFapsException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@EFapsUUID("c5bc9132-1f8a-44da-8780-3f63411a6607")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class AbstractRestClient
{
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRestClient.class);

    private static String ACCESSTOKEN;
    private static LocalDateTime ACCESSTOKENEXPIRES;

    protected Client getClient()
    {
        final ClientConfig clientConfig = new ClientConfig();
        try {
            final Class<?> clazz = Class.forName("org.efaps.esjp.logback.jersey.JerseyLogFeature");
            if (clazz != null) {
                final Object filter = clazz.getDeclaredConstructor().newInstance();
                final Method method = clazz.getMethod("setLogger", Logger.class);
                method.invoke(filter, LOG);
                clientConfig.register(filter);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
                        | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            LOG.error("Catched", e);
        }
        final Client client = ClientBuilder.newClient(clientConfig)
                        .register(JacksonFeature.class);
        return client;
    }

    protected void login()
        throws EFapsException
    {
        final var request = getClient().target(ElectronicBilling.FISCUS_SSO_ENDPOINTURI.get())
                        .path(ElectronicBilling.FISCUS_SSO_CLIENTID.get())
                        .path("/oauth2/token/")
                        .request(MediaType.APPLICATION_JSON);

        final var form = new Form()
                        .param("grant_type", "password")
                        .param("scope", "https://api-cpe.sunat.gob.pe")
                        .param("client_id", ElectronicBilling.FISCUS_SSO_CLIENTID.get())
                        .param("client_secret", ElectronicBilling.FISCUS_SSO_CLIENTSECRET.get())
                        .param("username", ElectronicBilling.FISCUS_SSO_USERNAME.get())
                        .param("password", ElectronicBilling.FISCUS_SSO_PWD.get());

        final var response  = request.buildPost(Entity.form(form)).invoke(new GenericType<SSOLoginResponseDto>()
        {
        });
        ACCESSTOKEN = response.getAccessToken();
        ACCESSTOKENEXPIRES = LocalDateTime.now().plusSeconds(response.getExpiresIn() - 10);
    }

    public String getToken()
        throws EFapsException
    {
        if (ACCESSTOKENEXPIRES == null || !LocalDateTime.now().isBefore(ACCESSTOKENEXPIRES)) {
            login();
        }
        return ACCESSTOKEN;
    }
}
