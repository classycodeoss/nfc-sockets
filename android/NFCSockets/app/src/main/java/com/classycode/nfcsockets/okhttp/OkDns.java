package com.classycode.nfcsockets.okhttp;

import android.util.Log;

import com.classycode.nfcsockets.minidns.NFCDnsSource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import de.measite.minidns.DNSCache;
import de.measite.minidns.DNSClient;
import de.measite.minidns.cache.LRUCache;
import de.measite.minidns.dnsserverlookup.AndroidUsingExec;
import de.measite.minidns.dnsserverlookup.AndroidUsingReflection;
import de.measite.minidns.dnsserverlookup.UnixUsingEtcResolvConf;
import de.measite.minidns.hla.ResolverApi;
import de.measite.minidns.hla.ResolverResult;
import de.measite.minidns.iterative.ReliableDNSClient;
import de.measite.minidns.record.A;
import de.measite.minidns.record.CNAME;
import de.measite.minidns.source.DNSDataSource;
import okhttp3.Dns;

/**
 * Custom DNS lookup based minidns library.
 *
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class OkDns implements Dns {

    private static final String TAG = OkDns.class.getSimpleName();

    private final ReliableDNSClient client;

    public OkDns() {

        // don't attempt to look up DNS servers, just use hard-coded defaults (Google)
        DNSClient.removeDNSServerLookupMechanism(AndroidUsingExec.INSTANCE);
        DNSClient.removeDNSServerLookupMechanism(AndroidUsingReflection.INSTANCE);
        DNSClient.removeDNSServerLookupMechanism(UnixUsingEtcResolvConf.INSTANCE);

        final DNSCache dnsCache = new LRUCache(1000);
        final DNSDataSource dnsDataSource = new NFCDnsSource();
        dnsDataSource.setTimeout(60 * 60 * 1000);
        client = new ReliableDNSClient(dnsCache);
        client.setDataSource(dnsDataSource);
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        final ResolverApi resolver = new ResolverApi(client);
        try {
            Log.d(TAG, "Attempting to resolve: " + hostname);
            final ResolverResult<A> result = resolver.resolve(hostname, A.class);
            if (result.wasSuccessful()) {
                if (result.getAnswers().isEmpty()) {
                    Log.w(TAG, "Empty result for A query: " + hostname + ", trying again for CNAME");
                    final ResolverResult<CNAME> cnameResult = resolver.resolve(hostname, CNAME.class);
                    if (cnameResult.wasSuccessful() && cnameResult.getAnswers() != null) {
                        final CNAME firstCname = cnameResult.getAnswers().iterator().next();
                        Log.d(TAG, "Got CNAME for " + hostname + ": " + firstCname.getTarget().asIdn());
                        return lookup(firstCname.getTarget().asIdn());
                    } else {
                        throw new UnknownHostException("Failed to resolve hostname: " + hostname);
                    }
                } else {
                    final List<InetAddress> addresses = new ArrayList<>(result.getAnswers().size());
                    for (A answer : result.getAnswers()) {
                        addresses.add(answer.getInetAddress());
                        Log.d(TAG, "Resolved " + hostname + " to InetAddress: " + answer.getInetAddress());
                    }
                    return addresses;
                }
            } else {
                throw new UnknownHostException("Failed to resolve hostname: " + hostname);
            }
        } catch (IOException e) {
            throw new UnknownHostException("Failed to resolve: " + hostname);
        }
    }
}
