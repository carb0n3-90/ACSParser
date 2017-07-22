package com.x.acs.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utility {
	private Utility() {
	}

	public static void unzip(final String zipFilename, String destDirname) throws IOException {

		final Path destDir = Paths.get(destDirname);
		// if the destination doesn't exist, create it
		if (Files.notExists(destDir)) {
			Files.createDirectories(destDir);
		}

		try (FileSystem zipFileSystem = createZipFileSystem(zipFilename, false)) {
			final Path root = zipFileSystem.getPath("/");
			// walk the zip file tree and copy files to the destination
			Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String newFileName = Paths.get(zipFilename).getFileName().toString();
					final Path destFile = Paths.get(destDir.toString(), newFileName);
					Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
					return FileVisitResult.TERMINATE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					final Path dirToCreate = Paths.get(destDir.toString(), dir.toString());
					if (Files.notExists(dirToCreate)) {
						Files.createDirectory(dirToCreate);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	private static FileSystem createZipFileSystem(String zipFilename, boolean create) throws IOException {
		// convert the filename to a URI
		final Path path = Paths.get(zipFilename);
		final URI uri = URI.create("jar:file:" + path.toUri().getPath());

		final Map<String, String> env = new HashMap<>();
		if (create) {
			env.put("create", "true");
		}
		return FileSystems.newFileSystem(uri, env);
	}

	public static void writeInFile(String fileName, String fileData) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
		out.println(fileData);
		out.close();
	}

	public static List<Path> readFilesFromDir(String sourceLoc, String criteria) throws IOException {
		List<Path> inputFiles = new ArrayList<>();
		DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(sourceLoc), criteria);
		for (Path path : stream) {
			inputFiles.add(path.toAbsolutePath());
		}
		stream.close();
		return inputFiles;
	}

	public static void recursiveDeleteOnExit(Path path) throws IOException {
		if (path != null) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					file.toFile().deleteOnExit();
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					dir.toFile().deleteOnExit();
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	public static void moveFile(String srcFile, String dstDir) throws IOException {
		new File(dstDir).mkdirs();
		Path srcFilePath = Paths.get(srcFile);
		Path tarFilePath = Paths.get(dstDir).resolve(srcFilePath.getFileName().toString());
		Files.move(srcFilePath, tarFilePath, StandardCopyOption.REPLACE_EXISTING);
	}

	public static boolean isEmptyStr(String str) {
		return str == null || str.isEmpty() || "null".equals(str);
	}
}
