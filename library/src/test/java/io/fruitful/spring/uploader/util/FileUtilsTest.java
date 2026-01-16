package io.fruitful.spring.uploader.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void safeDelete_shouldDeleteFileInsideBaseDir() throws IOException {
        File baseDir = tempDir.toFile();
        File fileToDelete = new File(baseDir, "test-file.txt");
        Assertions.assertTrue(fileToDelete.createNewFile());

        FileUtils.safeDelete(fileToDelete, baseDir);

        Assertions.assertFalse(fileToDelete.exists());
    }

    @Test
    void safeDelete_shouldNotDeleteFileOutsideBaseDir() throws IOException {
        Path outsideDir = Files.createTempDirectory("outside");
        try {
            File outsideFile = outsideDir.resolve("outside-file.txt").toFile();
            Assertions.assertTrue(outsideFile.createNewFile());

            // baseDir is tempDir, file is in outsideDir
            FileUtils.safeDelete(outsideFile, tempDir.toFile());

            Assertions.assertTrue(outsideFile.exists());
        } finally {
            // cleanup
            Files.deleteIfExists(outsideDir.resolve("outside-file.txt"));
            Files.deleteIfExists(outsideDir);
        }
    }

    @Test
    void safeDelete_shouldNotDeleteDirectory() throws IOException {
        File baseDir = tempDir.toFile();
        File subDir = new File(baseDir, "subdir");
        Assertions.assertTrue(subDir.mkdir());

        FileUtils.safeDelete(subDir, baseDir);

        Assertions.assertTrue(subDir.exists());
    }

    @Test
    void safeDelete_shouldNotDeleteSymlink() throws IOException {
        // Skip if symlinks not supported (e.g. some Windows setups without permissions)
        try {
            File baseDir = tempDir.toFile();
            File targetFile = new File(baseDir, "target.txt");
            Assertions.assertTrue(targetFile.createNewFile());

            Path linkPath = baseDir.toPath().resolve("link-to-target.txt");
            Files.createSymbolicLink(linkPath, targetFile.toPath());

            FileUtils.safeDelete(linkPath.toFile(), baseDir);

            Assertions.assertTrue(Files.exists(linkPath), "Symlink should still exist");
            Assertions.assertTrue(targetFile.exists(), "Target file should still exist");
        } catch (UnsupportedOperationException | IOException e) {
            // Symlinks might not be supported on the environment
            System.out.println("Skipping symlink test: " + e.getMessage());
        }
    }

    @Test
    void safeDelete_shouldHandleMissingFile() {
        File baseDir = tempDir.toFile();
        File missingFile = new File(baseDir, "missing.txt");

        Assertions.assertDoesNotThrow(() -> FileUtils.safeDelete(missingFile, baseDir));
    }

    @Test
    void safeDelete_shouldHandleNullBaseDir() throws IOException {
        // If baseDir is null, we might either allow unchecked delete OR block it.
        // The implementation checks "if (baseDir != null) ... check containment".
        // It then proceeds to check symlink/dir.
        // So with null baseDir, it behaves like a safer "deleteIfExists" that blocks
        // directory/symlink.

        File fileToDelete = new File(tempDir.toFile(), "null-base-test.txt");
        Assertions.assertTrue(fileToDelete.createNewFile());

        FileUtils.safeDelete(fileToDelete, null);

        Assertions.assertFalse(fileToDelete.exists());
    }
}
