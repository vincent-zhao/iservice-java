package src;

public class Constants {

	public static final int INI = 0;
	public static final int JSON = 1;
	public static final int DEFAULT = 2;
	
	public static final int SESSION_TIMEOUT = 10000;
	
	public static final long CHECK_TM = 15000;
	public static final int LOOP_INTERVAL = 3000;
	
	public static final int ZK_NOT_CONNECT = 0;
	public static final int ZK_OK = 1;
	public static final int ZK_ERROR = 2;
	public static final int ZK_EXPIRED = 3;
	
	public static final String GLOBAL_PREFIX = "global";
	
	public static final String ZK_PATH_ERROR_EVENT = "zk_path_error";
	public static final String DISCONNECT_EVENT = "disconnected";
	public static final String CONNECT_EVENT = "connected";
	public static final String EXPIRED_EVENT = "zk_expired";
	
	public static final String GET_DATA_ERROR_EVENT = "error";
	public static final String DATA_CHANGE_EVENT = "data_changed";
	public static final String DUMP_FAIL_EVENT = "dump_fail";
	public static final String DUMP_ERROR_EVENT = "dump_file_event";
	
	public static final String FILE_SUFFIX = ".data";
	public static final String MD5_SUFFIX = ".md5";
	
}
