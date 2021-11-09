package net.i2p.router;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import net.i2p.I2PAppContext;
import net.i2p.data.*;
import net.i2p.data.router.RouterAddress;
import net.i2p.data.router.RouterInfo;
import net.i2p.util.EepGet;
import net.i2p.util.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class utils {
    static I2PAppContext _context = I2PAppContext.getGlobalContext();
    static Log _log = _context.logManager().getLog(utils.class);
    static String BASE_64_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /**
     * http探针
     *
     * @param b32_destination b32编码的i2p地址
     * @param ls_json         json返回结果
     */
    public static void probHttp(String b32_destination, JsonObject ls_json) {
        String url = "http://" + b32_destination;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4 * 1024);
        EepGet get = new EepGet(_context, true, "127.0.0.1", 4444, 3, 0, 2 * 1024 * 1024,
            null, baos, url, false, null, null);
        _log.debug("http probing,url=" + url);
        if (get.fetch() && get.getStatusCode() == 200) {
            // HTTP
            ls_json.put("http", true);
            // ZZZOT
            String baosStr = baos.toString();
            _log.debug("http content：" + baosStr);
            if (baosStr.contains("ZZZOT")) {
                ls_json.put("zzzot", true);
            } else {
                ls_json.put("zzzot", false);
            }
            // tracker
            String trackerUrl = url + "/a";
            EepGet trackerGet = new EepGet(_context, true, "127.0.0.1", 4444, 3, 0, 2 * 1024 * 1024,
                null, baos, trackerUrl, false, null, null);
            if (trackerGet.fetch() && trackerGet.getStatusCode() == 200) {
                ls_json.put("tracker", true);
            } else {
                ls_json.put("tracker", false);
            }
        } else {
            _log.debug("http probing failed, stats:" + get.getStatusCode());
        }
    }

    /**
     * aof写日志文件
     *
     * @param file    日志绝对路径
     * @param content 日志内容
     */
    public static void aof(String file, String content) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
            out.write(content + "\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert out != null;
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 覆盖写日志文件
     *
     * @param file    日志绝对路径
     * @param content 日志内容
     */
    public static void rdb(String file, String content) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false)));
            out.write(content + "\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 本地格式化时间获取
     *
     * @return
     */
    public static String getLocalFormatTime() {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        return format.format(date);
    }

    /**
     * 时间戳转GMT格式化时间
     *
     * @param seconds 时间戳
     * @return
     */
    public static String timeStamp2Date(String seconds) {
        return timeStamp2Date(seconds, null);
    }

    public static String timeStamp2Date(String seconds, String format) {
        System.out.println(seconds);
        if (seconds == null || seconds.isEmpty() || seconds.equals("null")) {
            return "";
        }
        if (format == null || format.isEmpty()) {
            format = "yyyy/MM/dd HH:mm:ss.SSS";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date(Long.parseLong(seconds)));
    }

    /**
     * GMT格式化时间获取
     *
     * @return
     */
    public static String getFormatTime() {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }

    /**
     * GMT格式化日期获取
     *
     * @return
     */
    public static String getFormatDate() {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }

    /**
     * 获取日志目录
     * windows/linux
     *
     * @return
     */
    public static String getDataStoreDir() {
        String system = System.getProperty("os.name").toLowerCase();
        String path = null;
        if (system.startsWith("windows")) {
            path = "E:/i2pDataHouse/" + getFormatDate() + "/";
        } else {
            path = "/home/bf/workspace/i2pDataHouse/" + getFormatDate() + "/";
        }
        mkDirectories(path);
        return path;
    }

    /**
     * 创建文件夹
     *
     * @param path
     * @return
     */
    public static boolean mkDirectories(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                return file.mkdirs();
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("create dir " + path + " failed.");
        }
        return false;
    }

    /**
     * 根据i2p地址得到destination hash
     *
     * @param ip b32地址
     * @return
     */
    public static Hash getHashByIP(String ip) {
        if (ip == null) return null;
        ip = ip.replace('/', '~').replace('+', '-');
        if (ip.endsWith(".b32.i2p")) {
            byte[] b = Base32.decode(ip.substring(0, 52));
            if (b != null) {
                //Hash h = new Hash(b);
                Hash h = Hash.create(b);
                return h;
            }
        } else {
            try {
                Destination dst = new Destination(ip);
                return dst.getHash();
            } catch (DataFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 根据b32 ip构造Destination
     *
     * @param ip base64 destination
     * @return
     * @throws DataFormatException
     */
    public static Destination getDestinationByIP(String ip) throws DataFormatException {
        return new Destination(ip);
    }

    /**
     * ip: [0-9a-zA-Z]{52}.i2p
     * Base64 Hash or Hash.i2p or name.i2p using naming service
     */
    public static Destination getDestination(String ip) {
        if (ip == null) return null;
        if (ip.endsWith(".i2p")) {
            try {
                return new Destination(ip.substring(0, ip.length() - 4)); // sans .i2p
            } catch (DataFormatException dfe) {
                return null;
            }
        } else {
            try {
                return new Destination(ip);
            } catch (DataFormatException dfe) {
                return null;
            }
        }
    }

    /**
     * RouterInfo转json
     *
     * @param info
     * @return
     */
    public static JsonObject ri2json(RouterInfo info) {
        JsonObject riJson = new JsonObject();
        riJson.put("hash", info.getHash().toBase64());
        riJson.put("published", utils.timeStamp2Date(String.valueOf(info.getPublished())));
        riJson.put("caps", info.getCapabilities());
        riJson.put("version", info.getOption("router.version"));
        riJson.put("known_ls", info.getOption("netdb.knownLeaseSets"));
        riJson.put("known_ri", info.getOption("netdb.knownRouters"));
        JsonArray jsonArrAddress = new JsonArray();
        for (RouterAddress address : info.getAddresses()) {
            JsonObject addJson = new JsonObject();
            addJson.put("type", address.getTransportStyle());
            addJson.put("host", address.getHost());
            addJson.put("port", address.getPort());
            addJson.put("cost", address.getCost());
            addJson.put("caps", address.getOption("caps"));
            jsonArrAddress.add(addJson);
        }
        riJson.put("addresses", jsonArrAddress);
        return riJson;
    }

    /**
     * LeaseSet转json
     *
     * @param ls
     * @return
     */
    public static JsonObject ls2json(LeaseSet ls) {
        JsonObject lsJson = new JsonObject();
        lsJson.put("hash", ls.getHash().getData());
        lsJson.put("dest_b32", ls.getDestination().toBase32());
        lsJson.put("dest_b64", ls.getDestination().toBase64());
        lsJson.put("get_received_as_published", ls.getReceivedAsPublished());
        lsJson.put("get_received_by", ls.getReceivedBy());
        if (ls instanceof LeaseSet2) {
            lsJson.put("is_unpublished", ((LeaseSet2) ls).isUnpublished());
            lsJson.put("is_blinded", ((LeaseSet2) ls).isBlindedWhenPublished());
            lsJson.put("published", utils.timeStamp2Date(String.valueOf(((LeaseSet2) ls).getPublished())));
            lsJson.put("expires", utils.timeStamp2Date(String.valueOf(((LeaseSet2) ls).getExpires())));
        }
        Set<Hash> gws = new HashSet<>();
        for (int i = 0; i < ls.getLeaseCount(); i++) {
            gws.add(ls.getLease(i).getGateway());
        }
        lsJson.put("gws_hash", gws);
        return lsJson;
    }

    /**
     * 从本地文件加载获得RouterInfo信息
     *
     * @param riDatFile
     * @return
     */
    public static RouterInfo loadRIFromFile(String riDatFile) {
        InputStream fis = null;
        try {
            fis = new FileInputStream(riDatFile);
            fis = new BufferedInputStream(fis);
            RouterInfo info = new RouterInfo();
            info.readBytes(fis, true);
            return info;
        } catch (IOException | DataFormatException ioe) {
            System.out.println(ioe);
            return null;
        }
    }

    /**
     * @param netDBDir netDB目录的路径
     * @return netDB目录下的routerInfo.dat文件的数量
     */
    public static int getRICount(String netDBDir) {
        return getRICount(netDBDir, false);
    }

    /**
     * @param netDBDir netDB目录的路径
     * @param isFF     netDB目录下的属于ff的routerInfo.dat文件的数量
     * @return
     */
    public static int getRICount(String netDBDir, boolean isFF) {
        List<File> dirs = new LinkedList<File>();
        int rv = 0;
        try {
            File rootDir = new File(netDBDir);
            dirs.add(rootDir);
            while (!dirs.isEmpty()) {
                for (File f : dirs.get(0).listFiles()) {
                    if (f.isDirectory()) {
                        dirs.add(f);
                    } else if (f.isFile()) {
                        RouterInfo ri = loadRIFromFile(f.getAbsolutePath());
                        if (isFF && ri.getCapabilities().contains("f")) {
                            rv += 1;
                        } else if (!isFF) {
                            rv += 1;
                        }
                    }
                }
                dirs.remove(0);
            }
        } catch (Exception e) {
            return 0;
        }
        return rv;
    }

    /**
     * 计算当前peerProfile目录下的peerProfile文件数量
     *
     * @param p_dir peerProfile目录路径
     * @return
     */
    public static int getPeerProfilesCount(String p_dir) {
        List<File> dirs = new LinkedList<File>();
        int rv = 0;
        try {
            File rootDir = new File(p_dir);
            dirs.add(rootDir);
            while (!dirs.isEmpty()) {
                for (File f : dirs.get(0).listFiles()) {
                    if (f.isDirectory()) {
                        dirs.add(f);
                    } else if (f.isFile()) {
                        rv += 1;
                    }
                }
                dirs.remove(0);
            }
        } catch (Exception e) {
            return 0;
        }
        return rv;
    }

    /**
     * Destination hash转256位二进制
     *
     * @param hash_data
     * @return
     */
    public static String hash2binary(byte[] hash_data) {
        StringBuilder sb = new StringBuilder();
        for (byte h : hash_data) {
            sb.append(String.format("%8s", Integer.toBinaryString(h & 0xFF)).replace(" ", "0"));
        }
        return sb.toString();
    }

    /**
     * 将base64编码的字符串转化为256位01字符串
     *
     * @param b64 b64编码
     * @return
     */
    public static String base642binary(String b64) {
        char[] b64Chars = b64.toCharArray();
        StringBuilder b64Binary = new StringBuilder("");
        List<String> b64BinaryList = new ArrayList<>();
        for (char c : b64Chars) {
            if (c == '=') {
                continue;
            } else {
                String char2binStr = Integer.toBinaryString(BASE_64_MAP.indexOf(c));
                for (int i = 0; i < 6 - char2binStr.length(); i++) {
                    b64Binary.append("0");
                }
                b64Binary.append(char2binStr);
            }
        }
        for (int i = 0; i + 8 < b64Binary.length(); i += 8) {
            b64BinaryList.add(b64Binary.substring(i, i + 8));
        }
        return b64BinaryList.subList(0, 32).toString();
    }

    public static void main(String[] args) {
        try {
            System.out.println(getFormatTime());
            String p_dir = "C:\\Users\\DD12\\AppData\\Local\\I2P\\peerProfiles";
            System.out.println("当前拥有peerProfile数量为：" + getPeerProfilesCount(p_dir));
            String netDBDir = "C:\\Users\\DD12\\AppData\\Local\\I2P\\netDb";
            System.out.println("当前拥有RouterInfo数量为：" + getRICount(netDBDir));
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
