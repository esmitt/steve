/*
 * SteVe - SteckdosenVerwaltung - https://github.com/RWTH-i5-IDSG/steve
 * Copyright (C) 2013-2022 RWTH Aachen University - Information Systems - Intelligent Distributed Systems Group (IDSG).
 * All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.rwth.idsg.steve.ocpp.ws;

import de.rwth.idsg.steve.service.ChargePointHelperService;
import lombok.RequiredArgsConstructor;
import ocpp.cs._2015._10.RegistrationStatus;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.jetty.Jetty10RequestUpgradeStrategy;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 13.03.2015
 */
@RequiredArgsConstructor
public class OcppWebSocketUpgrader extends Jetty10RequestUpgradeStrategy {

    private final List<AbstractWebSocketEndpoint> endpoints;
    private final ChargePointHelperService chargePointHelperService;

    @Override
    public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
                        String selectedProtocol, List<WebSocketExtension> selectedExtensions, Principal user,
                        WebSocketHandler wsHandler, Map<String, Object> attributes){

        // -------------------------------------------------------------------------
        // 1. Check the chargeBoxId
        // -------------------------------------------------------------------------

        String chargeBoxId = getLastBitFromUrl(request.getURI().getPath());
        Optional<RegistrationStatus> status = chargePointHelperService.getRegistrationStatus(chargeBoxId);

        // Allow connections, if station is in db (registration_status field from db does not matter)
        boolean allowConnection = status.isPresent();

        if (allowConnection) {
            attributes.put(AbstractWebSocketEndpoint.CHARGEBOX_ID_KEY, chargeBoxId);
        } else {
            //throw new HandshakeFailureException("ChargeBoxId '" + chargeBoxId + "' is not recognized.");
            System.out.println("ChargeBoxId '" + chargeBoxId + "' is not recognized.");
            return;
        }

        // -------------------------------------------------------------------------
        // 2. Route according to the selected protocol
        // -------------------------------------------------------------------------

        if (selectedProtocol == null) {
            //throw new HandshakeFailureException("No protocol (OCPP version) is specified.");
            System.out.println("No protocol (OCPP version) is specified.");
            return;
        }

        AbstractWebSocketEndpoint endpoint = findEndpoint(selectedProtocol);

        if (endpoint == null) {
            //throw new HandshakeFailureException("Requested protocol '" + selectedProtocol + "' is not supported");
            System.out.println("Requested protocol '" + selectedProtocol + "' is not supported");
            return;
        }

        super.upgrade(request, response, selectedProtocol, selectedExtensions, user, endpoint, attributes);
    }

    @Nullable
    private AbstractWebSocketEndpoint findEndpoint(String selectedProtocol) {
        for (AbstractWebSocketEndpoint endpoint : endpoints) {
            if (endpoint.getVersion().getValue().equals(selectedProtocol)) {
                return endpoint;
            }
        }
        return null;
    }

    /**
     * Taken from: http://stackoverflow.com/a/4050276
     */
    private static String getLastBitFromUrl(final String url) {
        return url.replaceFirst(".*/([^/?]+).*", "$1");
    }
}
