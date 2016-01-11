package de.nrw.hbz.regal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ROUtils {

	public static String getStringFromInputStream(InputStream is) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();) {
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = is.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			is.close();
			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void stringToJsonFile(String jsonStr, File jsonFile) {
		try (FileWriter fw = new FileWriter(jsonFile)) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonObj = mapper.readTree(jsonStr);
			fw.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
