/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Radix IoT LLC,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.radixiot.com for commercial license options.
 */

package com.serotonin.bacnet4j.npdu.sc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

final class ConfiguredSSLSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory delegate;
    private final SSLParameters parameters;

    ConfiguredSSLSocketFactory(SSLSocketFactory delegate, SSLParameters parameters) {
        this.delegate = delegate;
        this.parameters = parameters;
    }

    private Socket configure(Socket socket) {
        if (socket instanceof SSLSocket s) {
            s.setSSLParameters(parameters);
        }
        return socket;
    }

    @Override
    public Socket createSocket() throws IOException {
        return configure(delegate.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return configure(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return configure(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        return configure(delegate.createSocket(host, port, localAddr, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return configure(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress host, int port, InetAddress localAddr, int localPort) throws IOException {
        return configure(delegate.createSocket(host, port, localAddr, localPort));
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }
}