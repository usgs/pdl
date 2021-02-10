package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.FileContent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.json.Json;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class DirectoryProductSourceTest {
  private static String JSON_EXAMPLE_PRODUCT = "etc/test_products/json/inline_content_example.json";
  private static String STORAGE_BASE_PATH = "product-digest-test-storage";

  /**
   * DirectoryProductSource attempts to convert URLContent to FileContent as part
   * of its `streamTo` method. This is typically fine, but if the URLContent is
   * for the empty path, this causes problems because a file does not exist on
   * disk and leads to FileNotFoundException downstream.
   *
   * This test ensures URLContent referencing the empty path is not converted to
   * FileContent. In this case, the URLContent _should_ typically reference a data
   * URL.
   */
  @Test
  public void directoryProductSource() {
    try (InputStream fin = new FileInputStream(JSON_EXAMPLE_PRODUCT);
        InputStream in = StreamUtils.getInputStream(fin);) {
      File storageDirectory = new File(STORAGE_BASE_PATH);

      // Create product from JSON file and stream to directory [as XML]
      Product jsonProduct = new JsonProduct().getProduct(Json.createReader(in).readObject());
      DirectoryProductHandler outputHandler = new DirectoryProductHandler(storageDirectory);
      ObjectProductSource outputSource = new ObjectProductSource(jsonProduct);
      outputSource.streamTo(outputHandler);

      // Create product from directory
      ObjectProductHandler inputHandler = new ObjectProductHandler();
      DirectoryProductSource inputSource = new DirectoryProductSource(storageDirectory);
      inputSource.streamTo(inputHandler);
      Product directoryProduct = inputHandler.getProduct();

      // Ensure the inline (empty path) content is not converted to file content
      Content content = directoryProduct.getContents().get("");
      Assert.assertNotNull(content);
      Assert.assertTrue("Empty path content not of type FileContent", !(content instanceof FileContent));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  /**
   * Clean up the temporary storage directory after test(s).
   *
   */
  @After
  public void cleanup() {
    // Cleanup tmp storage directory
    File storageDirectory = new File(STORAGE_BASE_PATH);
    if (storageDirectory.isDirectory()) {
      deleteDirectory(storageDirectory);
    }
  }

  /**
   * Helper method to recursively delete a directory (or file).
   *
   * @param file The directory (or file) to delete.
   */
  private boolean deleteDirectory(File directory) {
    File[] files = directory.listFiles();

    if (files != null) {
      for (File file : files) {
        deleteDirectory(file);
      }
    }

    return directory.delete();
  }
}