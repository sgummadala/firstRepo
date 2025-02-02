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
import javax.xml.bind.DatatypeConverter ;

import org.json.JSONObject;

public class CompressJson {

	// /Users/gummadala/Documents/Java/JavaFiles
	
	
	public static File inFolder = new File(
		/*	"C:\\UnCompressedFiles"); */
			 
			"/Users/gummadala/Documents/Java/JavaFiles/UnCompressed");
	public static File outFolder = new File(
		/*	 "C:\\CompressedFiles"); */
            "/Users/gummadala/Documents/Java/JavaFiles/Compressed");
	public static String inFileName = null;
	public static String outFileName = null;
	public static String absInPath = null;
	public static String absOutPath = null;

	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		absInPath = inFolder.getAbsolutePath();
		String absFileName = null;
		String absOutFileName = null;
		String out1 = "{\"storeTlogRequest\":{\"ver\":5,\"msgStrNum\":\"";
		String msgStrNum = null;
		String out2 = "\"msgStrNum\":\"";
		String msgSeqNum = null;
		String out3 = "\",\"msgSeqNum\":\"";
		BigInteger regNum = null;
		String out4 = "\", \"regNum\":";
		BigInteger txnNum = null;
		String out5 = ",\"txnNum\":\"";
		String txndata = null;
		String out6 = "\",\"txndata\":\"";
		String txnTmStmp = null;
		String out7 = "\",\"txnTmStmp\":\"";
		String out8 = "\"}}";
		for (final File fileEntry : inFolder.listFiles()) {
			if (fileEntry.isFile()) {
				inFileName = fileEntry.getName();

				absFileName = absInPath + "/" + inFileName;

				// Read File
				String json = readFile(absFileName);
				System.out.println(json);

				JSONObject jsonObj = new JSONObject(json);
				JSONObject StrTlogTxn = jsonObj.getJSONObject("StrTlogTxn");
				JSONObject strTlogRec = StrTlogTxn.getJSONObject("strTlogRec");
				JSONObject tlogMsgHeader = strTlogRec
						.getJSONObject("tlogMsgHeader");
				// JSONObject strTlogRec =
				// StrTlogTxn.getJSONObject("strTlogRec");

				String storeNumber = StrTlogTxn.getString("msgStrNum");
				String seqNum = StrTlogTxn.getString("msgSeqNum");
				BigInteger register = tlogMsgHeader.getBigInteger("regNum");

				// getString("regNum");
				BigInteger tranNum = tlogMsgHeader.getBigInteger("txnNum");
				String dateTime = "";
				dateTime = tlogMsgHeader.getString("dateTime");

				byte a[] = compress(json);
				String compressString = DatatypeConverter.printBase64Binary(a);

				// String deCompressString =
				// DatatypeConverter.printBase64Binary(json.getBytes());

				// Write XML File
				absOutPath = outFolder.getAbsolutePath();
				//absOutFileName = absOutPath + "/" + "Compressed-" + inFileName;
				absOutFileName = absOutPath + "/" + inFileName;
				System.out.println("Out File name is : " + absOutFileName);
				msgStrNum = storeNumber;
				msgSeqNum = seqNum;
				regNum = register;
				txnNum = tranNum;
				txnTmStmp = dateTime; // "2017-08-14 12:04:00 AM";
				txndata = compressString;
				String output = out1 + msgStrNum + out3 + msgSeqNum + out4
						+ regNum + out5 + txnNum + out6 + txndata + out7
						+ txnTmStmp + out8;
				System.out.println("Content is : " + output);
				writeFile(absOutFileName, output);
				// writeFile(absOutFileName, deCompressString);
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
