package org.ngelmakproject.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Sevice for saving and retrieving files.
 * 
 * Learn more about Spring-Boot storage please check:
 * https://spring.io/guides/gs/uploading-files
 * 
 * @author yusufaye
 */
@Service
public class FileStorageService {

  @Value("${nk.file.upload-directory.location}")
  private String location;
  @Value("${nk.file.public.access.location}")
  private String publicAccessLocation;
  @Value("${nk.file.private.access.location}")
  private String privateAccessLocation;

  @Value("${server.host}")
  private String host;
  @Value("${server.port}")
  private Integer port;
  @Value("${server.protocol}")
  private String protocol;

  public Path root(String... dirs) {
    return Paths.get(location, dirs).toAbsolutePath();
  }

  public void init(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new StorageException("Could not initialize storage", e);
    }
  }

  /**
   * Save file to the local folder.
   * 
   * @param file
   */
  public final URL store(MultipartFile file, boolean isPublic, String filename, String... dirs) {
    try {
      if (file.isEmpty()) {
        throw new StorageException("Failed to store empty file.");
      }
      Path destinationFile = root(dirs).resolve(filename).normalize();
      if (!Files.exists(destinationFile.getParent())) {
        this.init(destinationFile.getParent());
      }
      try (InputStream inputStream = file.getInputStream()) {
        Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        return toUrl(destinationFile, isPublic);
      }
    } catch (IOException e) {
      throw new StorageException("Failed to store file: " + filename, e);
    }
  }

  public Stream<Path> loadAll(String... dirs) throws IOException {
    final Path location = root(dirs);
    try {
      return Files.walk(location, 1)
          .filter(path -> !path.equals(location))
          .map(location::relativize);
    } catch (IOException e) {
      throw new StorageException("Failed to read stored files", e);
    }
  }

  public Resource loadAsResource(String url) throws IOException {
    try {
      Path file = toPath(new URL(url));
      Resource resource = new UrlResource(file.toUri());
      if (resource.exists() || resource.isReadable()) {
        return resource;
      } else {
        throw new StorageFileNotFoundException("Could not read file: " + url);
      }
    } catch (MalformedURLException e) {
      throw new StorageFileNotFoundException("Could not read file: " + url, e);
    }
  }

  public void delete(URL url) {
    try {
      FileSystemUtils.deleteRecursively(toPath(url));
    } catch (IOException e) {
      throw new StorageFileNotFoundException("Error when deleting file : " + url, e);
    }
  }

  public void delete(String url) {
    try {
      delete(new URL(url));
    } catch (MalformedURLException e) {
      throw new StorageFileNotFoundException("Error when deleting file : " + url, e);
    }
  }

  /**
   * Change given path to url.
   * The absolute path is splited to extract the workdir (location) and replace it
   * with the public/private representation.
   * 
   * @param path     is the absolute path of the file.
   * @param isPublic
   * @return
   * @throws MalformedURLException
   */
  private URL toUrl(Path path, boolean isPublic) throws MalformedURLException {
    path = root().relativize(path); // child path
    String file = String.format("/%s/%s", isPublic ? publicAccessLocation : privateAccessLocation, path.toString());
    file = clean(file);
    return new URL(this.protocol, this.host, this.port, file);
  }

  private Path toPath(URL url) throws MalformedURLException {
    String file = url.getFile();
    file = file.replace(this.privateAccessLocation, "").replace(this.publicAccessLocation, "");
    file = clean(file);
    return root(file);
  }

  private String clean(String path) {
    return path.replace("//", "/").replace("/./", "/");
  }
}
