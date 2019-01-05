package com.gallantrealm.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.gallantrealm.mysynth.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FileSelectorDialog extends Dialog {

	public interface SelectionListener {
		public void onFileselected(String filename);
	}

	protected Activity activity;
	protected ArrayAdapter<String> fileListAdapter;
	protected SelectionListener selectionListener;
	protected String initialFolder;
	protected String currentFolder;
	protected List<String> files = new ArrayList<String>();
	protected String extension;

	public FileSelectorDialog(Activity activity, String extension) {
		super(activity, R.style.Theme_Dialog);
		this.activity = activity;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.fileselector_dialog);
		setCancelable(true);
		setCanceledOnTouchOutside(true);
		this.extension = extension;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Translator.getTranslator().translate(this.getWindow().getDecorView());

		final ListView fileListView = (ListView) findViewById(R.id.fileListView);
		fileListAdapter = createListAdapter(new ArrayList<String>());
		fileListView.setAdapter(fileListAdapter);

		fileListView.setOnItemClickListener(new OnItemClickListener() {
			@SuppressLint("NewApi")
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final String selection = "" + fileListAdapter.getItem(position);
				System.out.println("FileSelectorDialog.onItemClick: " + selection + " in folder: " + currentFolder);
				if (selection.length() > 0 && selection.charAt(selection.length() - 1) == '/') { // a folder
					currentFolder = currentFolder + selection;
					updateDirectory();
					fileListView.scrollBy(0, 1);
					fileListView.scrollBy(0, -1);
				} else if (selection.equals("..")) { // move up
					if (currentFolder.contains("/")) {
						currentFolder = currentFolder.substring(0, currentFolder.lastIndexOf("/"));
						if (currentFolder.contains("/")) {
							currentFolder = currentFolder.substring(0, currentFolder.lastIndexOf("/") + 1);
						} else {
							currentFolder = "";
						}
					} else {
						currentFolder = "";
					}
					updateDirectory();
					fileListView.scrollBy(0, 1);
					fileListView.scrollBy(0, -1);
				} else { // a file
					dismiss();
					Thread thread = new Thread() {
						public void run() {
							try {
								String filename;
								if (extension != null) {
									filename = selection + extension;
								} else {
									filename = selection;
								}
								selectionListener.onFileselected(currentFolder + filename);
							} catch (Exception e) {
								e.printStackTrace();
							}
							completeFileRead();
						}
					};
					thread.start();
				}
			}
		});
	}

	private ArrayAdapter<String> createListAdapter(List<String> items) {
		return new FileSelectorAdapter(getContext(), R.layout.fileselector_item, items) {
		};
	}

	class FileSelectorAdapter extends ArrayAdapter<String> {
		LayoutInflater inflater;
		private Typeface iconTypeface;

		public FileSelectorAdapter(Context context, int resource, List<String> items) {
			super(context, resource, items);
			inflater = LayoutInflater.from(context);
			iconTypeface = Typeface.createFromAsset(getContext().getAssets(), "fontawesome-webfont.ttf");
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			if (convertView != null) {
				v = convertView;
			} else {
				v = FileSelectorAdapter.this.inflater.inflate(R.layout.fileselector_item, parent, false);
			}
			String filename = FileSelectorAdapter.this.getItem(position).trim();
			TextView icon = (TextView) v.findViewById(R.id.icon);
			icon.setTypeface(iconTypeface);
			TextView text = (TextView) v.findViewById(R.id.text);
			if (filename.endsWith("/")) {
				icon.setText(getContext().getString(R.string.icon_folder));
			} else {
				icon.setText(getContext().getString(R.string.icon_file_o));
			}
			text.setText(filename);

			return v;
		}

	}

	/**
	 * Use this to show the dialog
	 * 
	 * @param selectionListener
	 */
	public void show(String initialFolder, SelectionListener selectionListener) {
		this.initialFolder = initialFolder;
		currentFolder = initialFolder;
		this.selectionListener = selectionListener;
		updateDirectory();
		super.show();
	}

	@SuppressLint("NewApi")
	private void updateDirectory() {
		Thread thread = new Thread() {
			public void run() {
				try {
					String folder = currentFolder;
					files.clear();
					if (!currentFolder.equals(initialFolder)) {
						files.add("..");
					}
					files.addAll(getFileNames(folder));
					activity.runOnUiThread(new Runnable() {
						public void run() {
							fileListAdapter.clear();
							fileListAdapter.addAll(files);
							fileListAdapter.notifyDataSetChanged();
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					activity.runOnUiThread(new Runnable() {
						public void run() {
							MessageDialog connectFailed = new MessageDialog(getContext(), "Connect Failed", "Cannot access the library.  Check connection to the internet.", new String[] { "OK" });
							connectFailed.show();
							FileSelectorDialog.this.dismiss();
						}
					});
				}
			}
		};
		thread.start();
	}

	protected List<String> getFileNames(String folder) throws Exception {
		List<String> fileNames = new ArrayList<String>();
		File folderFile = new File(folder);
		if (!folderFile.exists() || !folderFile.isDirectory()) {
			return fileNames;
		}
		for (File file : folderFile.listFiles()) {
			if (file.isDirectory()) {
				// Add "/" to directory names to identify them in the list
				fileNames.add(file.getName() + "/");
			} else if (extension == null || file.getName().endsWith(extension)) {
				if (extension == null) {
					fileNames.add(file.getName());
				} else {
					fileNames.add(file.getName().substring(0, file.getName().indexOf(extension)));
				}
			}
		}
		Collections.sort(fileNames, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});
		return fileNames;
	}

	public InputStream getFileInputStream(String file) throws IOException {
		return new FileInputStream(file);
	}

	protected void completeFileRead() {
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
			System.out.println(currentFolder);
			if (currentFolder != "") {
				if (currentFolder.contains("/")) {
					currentFolder = currentFolder.substring(0, currentFolder.lastIndexOf("/"));
					if (currentFolder.contains("/")) {
						currentFolder = currentFolder.substring(0, currentFolder.lastIndexOf("/") + 1);
					} else {
						currentFolder = "";
					}
				} else {
					currentFolder = "";
				}
				updateDirectory();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

}
