package com.gallantrealm.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import android.app.Activity;

public class FtpFileSelectorDialog extends FileSelectorDialog {

	FTPClient ftpClient = new FTPClient();
	String hostname;
	String userid;
	String password;

	public FtpFileSelectorDialog(Activity activity, final String hostname, final String userid, final String password, String extension) {
		super(activity, extension);
		this.hostname = hostname;
		this.userid = userid;
		this.password = password;
	}

	@Override
	protected List<String> getFileNames(String folder) throws Exception {
		List<String> fileNames = new ArrayList<String>();
		System.out.println("CONNECTING TO " + hostname + " AS " + userid);
		connectIfNeeded();
		ftpClient.changeWorkingDirectory(folder);
		System.out.println("LISTING FILES FOR " + folder);
		FTPFile[] files = ftpClient.listFiles();
		for (FTPFile file : files) {
			if (!file.getName().startsWith(".")) {
				if (file.isDirectory()) {
					fileNames.add(file.getName() + "/");
				} else if (extension == null) {
					fileNames.add(file.getName());
				} else if (file.getName().endsWith(extension)) {
					fileNames.add(file.getName().substring(0, file.getName().indexOf(extension)));
				}
			}
		}
		System.out.println("" + files.length + " FILES");
		Collections.sort(fileNames, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});
		return fileNames;
	}

	private void connectIfNeeded() throws IOException {
		if (!ftpClient.isConnected()) {
			int retries = 0;
			while (retries < 4) {
				try {
					ftpClient.connect(hostname);
					ftpClient.enterLocalPassiveMode();
					ftpClient.login(userid, password);
					ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
					ftpClient.setKeepAlive(true);
					return;
				} catch (Exception e) {
					if (retries >= 4) {
						e.printStackTrace();
						return;
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e2) {
					}
					retries++;
				}
			}
		}
	}

	public InputStream getFileInputStream(String file) throws IOException {
		connectIfNeeded();
		return ftpClient.retrieveFileStream(file);
	}

	protected void completeFileRead() {
		try {
			ftpClient.completePendingCommand();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
