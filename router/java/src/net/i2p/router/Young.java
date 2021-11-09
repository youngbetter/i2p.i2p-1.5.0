package net.i2p.router;

import net.i2p.I2PAppContext;
import net.i2p.data.*;
import net.i2p.data.Base64;
import net.i2p.data.router.RouterInfo;
import net.i2p.kademlia.KBucketSet;
import net.i2p.util.Log;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static net.i2p.router.utils.*;


public class Young {
    private static final int K = 24;
    private static final int B = 4;
    private static final String FAKE_MIN_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    private static final String _routerFile = "C:\\Users\\DD12\\AppData\\Roaming\\I2P\\netDB\\rb\\routerInfo-BHMdGW1RWcd1L~ZRlusWxIh3zOfwy1CnypXassz2Q1U=.dat";
    static I2PAppContext context;
    private static Log log;
    private static final String FAKE_MAX_KEY = Hash.create(Young.getRemotestFromUs(Young.getHash(FAKE_MIN_KEY).getData())).toBase64();
    private final KBucketSet<Hash> kBucketSet;
    private final Hash usHash;
    private RouterInfo usInfo;

    public Young(String key) {
        context = I2PAppContext.getGlobalContext();
        log = context.logManager().getLog(KBucketSet.class);
        InputStream fis = null;
        try {
            fis = new FileInputStream(_routerFile);
            fis = new BufferedInputStream(fis);
            usInfo = new RouterInfo();
            usInfo.readBytes(fis, true);  // true = verify sig on read
        } catch (IOException | DataFormatException ioe) {
            log.error("load routerInfo error");
        }
        // We use the default RandomTrimmer so add() will never fail
        usHash = getHash(key);
        kBucketSet = new KBucketSet<Hash>(context, usHash, K, B);
    }

    /**
     * @param key
     * @return hash
     */
    public static Hash getHash(String key) {
        try {
            // fastjson convert
            key = key.replace('/', '~').replace('+', '-');
            byte[] b = Base64.decode(key);
            if (b == null)
                return null;
            Hash h = Hash.create(b);
            return h;
        } catch (RuntimeException dfe) {
            log.warn("Invalid base64 [" + key + "]", dfe);
            return null;
        }
    }

    public static byte[] getRemotestFromUs(byte[] hash_data) {
        byte[] rv = new byte[hash_data.length];
        for (int i = 0; i < hash_data.length; i++) {
            rv[i] = (byte) ~hash_data[i];
        }
        return rv;
    }

    public static void cal_distance(String key, String ff_stats) {
        Young y = new Young(key);
        BigInteger big_us = new BigInteger(1, y.usHash.getData());
        JsonArray j_arr = new JsonArray();
        Hash hash_remotest = new Hash(getRemotestFromUs(y.usHash.getData()));
        BigDecimal remotest_xor = new BigDecimal(big_us.xor(new BigInteger(1, hash_remotest.getData())));
        try {
            BufferedReader in = new BufferedReader(new FileReader(ff_stats));
            String str;
            while ((str = in.readLine()) != null) {
                JsonObject item = new JsonObject();
                Hash peerHash = y.getHash(str);
                item.put("r_key", key);
                item.put("peer", peerHash.toBase64());
                item.put("xor", y.getXorWith(peerHash));
                BigDecimal tmp_xor = new BigDecimal(big_us.xor(new BigInteger(1, peerHash.getData())));
                item.put("dis", tmp_xor.divide(remotest_xor, 20, BigDecimal.ROUND_HALF_UP));
                item.put("b_xor", big_us.xor(new BigInteger(1, peerHash.getData())).divide(new BigInteger("1000000000000000000000000000000000000000000000000000000000000000000000000000")));
                j_arr.add(item);
            }
        } catch (IOException e) {
            System.out.println("some error" + e);
        }
        utils.rdb(utils.getDataStoreDir() + "ff_dis_" + ff_stats.split("\\\\")[2], j_arr.toJson());
        System.out.println("Happy new year");
    }

    public static void cal_distance_abs(String ff_stats) {
        List<String> peers = sortedPeers(ff_stats);
        peers.add(Young.FAKE_MAX_KEY);
        Young pre = new Young(Young.FAKE_MIN_KEY);
        JsonArray j_arr = new JsonArray();
        for (String peer : peers) {
            BigInteger big_pre = new BigInteger(1, pre.usHash.getData());
            JsonObject item = new JsonObject();
            Hash peerHash = Young.getHash(peer);
            item.put("pre_peer", pre.usHash.toBase64());
            item.put("cur_peer", peerHash.toBase64());
            item.put("xor", pre.getXorWith(peerHash));
            item.put("b_xor", big_pre.xor(new BigInteger(1, peerHash.getData())).divide(new BigInteger("100000000000000000000000000000000000000000000000000000000000000000000")));
            j_arr.add(item);
            pre = new Young(peer);
        }
        utils.rdb(utils.getDataStoreDir() + "ff_abs_dis_" + ff_stats.split("\\\\")[2], j_arr.toJson());
        System.out.println("Happy new year");
    }

    public static void cal_p(String ff_stats) {
        List<String> peers = sortedPeers(ff_stats);
        JsonArray j_arr = new JsonArray();
        for (String peer : peers) {
            List<Integer> xors = new ArrayList<>();
            for (String pp : peers) {
                if (peer.equals(pp))
                    continue;
                Young y_pp = new Young(pp);
                xors.add(y_pp.getXorWith(Young.getHash(peer)));
            }
            Collections.sort(xors);
            JsonObject item = new JsonObject();
            Hash peerHash = Young.getHash(peer);
            assert peerHash != null;
            item.put("cur_peer", peerHash.toBase64());
            item.put("p", xors);
            j_arr.add(item);
        }
        utils.rdb(utils.getDataStoreDir() + "ff_p" + ff_stats.split("\\\\")[2], j_arr.toJson());
        System.out.println("Happy new year");
    }

    public static void convert_h2b(String ff_stats) {
        List<String> peers = sortedPeers(ff_stats);
        JsonArray j_arr = new JsonArray();
        for (String peer : peers) {
            j_arr.add(hash2binary(Young.getHash(peer).getData()));
        }
        utils.rdb(utils.getDataStoreDir() + "binary_peers" + ff_stats.split("\\\\")[2], j_arr.toJson());
        System.out.println("Happy new year");
    }

    public static List<String> sortedPeers(String ff_path) {
        List<String> rv = new ArrayList<>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(ff_path));
            String str;
            while ((str = in.readLine()) != null) {
                rv.add(str);
            }
        } catch (IOException e) {
            System.out.println("some error" + e);
        }
        rv.sort(Comparator.comparing(p -> hash2binary(Objects.requireNonNull(Young.getHash(p)).getData())));
        return rv;
    }

    public static void main(String[] args) throws DataFormatException {
//        String b32 = "a5ucro55axbgpbrogwkd3cg4f43f5r4kflarsk5vyublx5xax5ga.b32.i2p";
//        System.out.println(b32.substring(0, 52));
//        String des = "JgWcPdKxAWgQK6tYi3aM~7ZJi6nDaDZOcijl25trW0yGp1ZFN9dbX20bQ~Ms8K2wbCVPGJL5oOJUh0gSvW~IYI08AHaMwl-cCpx8uqPZROVMrCweSHdsioB9tOz1y6SJIGv49d02YX0aQdTfKHYPepPNzOOU6PFew3MxhDoIa-Fxqs9JmhN3KL-zs3MFCuVviIO5MhhBMnKAJWUcERfshO2dAubfapO0ww-M7jAMl14MCHTfr28CICJFl5hgvZ3CNFZhKp8aVa1-gOc4ul5Vkl9KoxqNFMmJzo5Y5ZLACxoGlaS7cS62XhqE-Jk87FgeCZU0cMgsPqv2GeTG1HMmlZ6dST3w9VQyAh4VdOFKmsab23YHFO8-CokiZjMI7a3G~go1ZglXb7Ey-3Y22W2Ch2PIXcsQffyOAx0woU5mdQhYdyJPh2lhvxDJwITyztLf6W76Alen46Yt~kFV8fYbAiognJ2EhjbNCf6PsXwX~2rAdC2c~pdtAwXq~d4jDN7uBQAEAAcAAA==";
//        Destination d = getDestination(des);
//        System.out.println(d.getHash());
//        System.out.println(getHash("rWZMOblslnhKzTci1SKINubhLA4tuOqdKDExTwzjRSI="));
        RouterInfo ri = loadRIFromFile("C:\\Users\\DD12\\AppData\\Local\\I2P\\netDb\\rB\\routerInfo-B8yXBh-EPw-bRVi0Kk8R8cIYUIf87MfyWGeKp0--k3o=.dat");
        System.out.println(ri);
        if(ri.isValid()){
            System.out.println("ri is valid");
        }else{
            System.out.println("ri is not valid");
        }
//        assert ri != null;
//        System.out.println("**********************************");
//        ri.setCapacities("Hello");
//        System.out.println(ri);;
//        System.out.println(getDestination("JgWcPdKxAWgQK6tYi3aM~7ZJi6nDaDZOcijl25trW0yGp1ZFN9dbX20bQ~Ms8K2wbCVPGJL5oOJUh0gSvW~IYI08AHaMwl-cCpx8uqPZROVMrCweSHdsioB9tOz1y6SJIGv49d02YX0aQdTfKHYPepPNzOOU6PFew3MxhDoIa-Fxqs9JmhN3KL-zs3MFCuVviIO5MhhBMnKAJWUcERfshO2dAubfapO0ww-M7jAMl14MCHTfr28CICJFl5hgvZ3CNFZhKp8aVa1-gOc4ul5Vkl9KoxqNFMmJzo5Y5ZLACxoGlaS7cS62XhqE-Jk87FgeCZU0cMgsPqv2GeTG1HMmlZ6dST3w9VQyAh4VdOFKmsab23YHFO8-CokiZjMI7a3G~go1ZglXb7Ey-3Y22W2Ch2PIXcsQffyOAx0woU5mdQhYdyJPh2lhvxDJwITyztLf6W76Alen46Yt~kFV8fYbAiognJ2EhjbNCf6PsXwX~2rAdC2c~pdtAwXq~d4jDN7uBQAEAAcAAA=="));
    }

    public KBucketSet<Hash> getBucketSet() {
        return kBucketSet;
    }

    public RouterInfo getUsInfo() {
        return usInfo;
    }

    public int getXorWith(Hash peer) {
        return peer == null ? Integer.MIN_VALUE : getBucketSet().getRange(peer);
    }
}
