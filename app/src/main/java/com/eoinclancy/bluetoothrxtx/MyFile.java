package com.eoinclancy.bluetoothrxtx;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class MyFile {

	private File root;
	private File file;
	private BufferedWriter out;

	public MyFile() {
		root = Environment.getExternalStorageDirectory();
	}

	public void createFile(String fileName) {
		try {
			if (root.canWrite()) {
				file = new File(root, "//" + fileName);
				if (!file.exists()) {
					file.createNewFile();
				}
			}
		} catch (IOException e) {
			Log.e("Error", "fail to create a new file");
		}

	}

	public StringBuilder read(String fileName) {

		StringBuilder text = new StringBuilder();		//Used for storing the entire read-in text
		String line = "";								//Used for storing each line as they are read in
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));	//Reads text from character input stream

			while ((line = br.readLine()) != null) {	//While another line exists, assign it to the 'text' variable
				text.append(line);
				text.append('\n');
			}
			br.close();
		}
		catch (IOException e) {
			Log.e("Error", "failed to read from file");
		}
		return text;									//Return the BufferedReader
	}


	public void write(String message) {
		try {
			if (out == null) {
				FileWriter datawriter = new FileWriter(file);
				out = new BufferedWriter(datawriter);
			}
			if (file.exists()) {
				out.write(message);
				out.flush();
			}
		} catch (IOException e) {
			Log.e("Error", "fail to write file");
		}
	}

	public void close() {
		try {
			out.close();
		} catch (IOException e) {
			Log.e("Error", "fail to close file");
		}
	}

}
