package gov.usgs.earthquake.aws;

import gov.usgs.earthquake.product.Product;

public class ProductAlreadySentException extends Exception {

	private static final long serialVersionUID = 1L;

  public final Product product;

  public ProductAlreadySentException(final Product product) {
    this.product = product;
  }
}
