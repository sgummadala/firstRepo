package com.cvs.pos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.DatatypeConverter;

import org.json.JSONObject;
import org.json.XML;

public class Decompress_test {

	public static File inFolder = new File(
			//"C:\\Users\\C006810\\Documents\\01-ITPR019856-CAR Documents\\00-UnitTesting\\SI008\\T\\Comp");
			//"C:\\Java\\SI008\\input");
			"C:\\CompressedFiles");
	public static File outFolder = new File(
			//"C:\\Users\\C006810\\Documents\\01-ITPR019856-CAR Documents\\00-UnitTesting\\SI008\\T\\Decomp");
			//"C:\\Java\\SI008\\output");
			"C:\\UnCompressedFiles");
	public static String inFileName = null;
	public static String outFileName = null;
	public static String absInPath = null;
	public static String absOutPath = null;

	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		absInPath = inFolder.getAbsolutePath();
		String absFileName = null;
		String absOutFileName = null;
		String compressed = "";

		for (final File fileEntry : inFolder.listFiles()) {
			if (fileEntry.isFile()) {
				inFileName = fileEntry.getName();
				absFileName = absInPath + "/" + inFileName;

				// Read File
				String json = readFile(absFileName);

				JSONObject jsonObj = new JSONObject(json);

				JSONObject storeTlogRequest = jsonObj
						.getJSONObject("storeTlogRequest");
				compressed = storeTlogRequest.getString("txndata");

				String decompressed = null;
				try {
					decompressed = decompress(DatatypeConverter
							.parseBase64Binary(compressed));

				} catch (IOException e) {
				}

				JSONObject txndata_json = new JSONObject(decompressed);
				// String txndata_xml = XML.toString(txndata_json);
				String storeNumber = storeTlogRequest.getString("msgStrNum");
				BigInteger tranNum = storeTlogRequest.getBigInteger("txnNum");

				// Write XML File
				absOutPath = outFolder.getAbsolutePath();
				absOutFileName = absOutPath + "/" +  inFileName ;
				System.out.println("Out File name is : " + absOutFileName);

				String output = txndata_json.toString(4);
				System.out.println("Content is : " + output);
				writeFile(absOutFileName, output);
			}
		}
	}

	public static String readFile(String filepath)
			throws FileNotFoundException, IOException {

		StringBuilder sb = new StringBuilder();
		InputStream in = new FileInputStream(filepath);
		Charset encoding = Charset.defaultCharset();

		Reader reader = new InputStreamReader(in, encoding);

		int r = 0;
		while ((r = reader.read()) != -1)// Note! use read() rather than
		// readLine()
		// Can process much larger files
		// with read()
		{
			char ch = (char) r;
			sb.append(ch);
		}

		in.close();
		reader.close();

		return sb.toString();
	}

	public static void writeFile(String filepath, String output)
			throws FileNotFoundException, IOException {
		FileWriter ofstream = new FileWriter(filepath);
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(ofstream);
			out.write(output);
		} catch (Exception e) {
			System.out.println(e.toString());
		} finally {
			out.flush();
		}
	}

	public static byte[] compress(final String str) throws IOException {
		if ((str == null) || (str.length() == 0)) {
			return null;
		}
		ByteArrayOutputStream obj = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(obj);
		gzip.write(str.getBytes("UTF-8"));
		gzip.close();
		return obj.toByteArray();
	}

	public static String decompress(final byte[] compressed) throws IOException {
		String outStr = "";
		if ((compressed == null) || (compressed.length == 0)) {
			return "";
		}
		
		if (isCompressed(compressed)) {
			GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(
					compressed));
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(gis, "UTF-8"));

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				outStr += line;
			}
		} else {
			outStr = new String(compressed);
		}
		return outStr;
	}

	public static boolean isCompressed(final byte[] compressed) {
		return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
				&& (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
	}
}
