/*
 * ProductId
 */
package gov.usgs.earthquake.product;


import java.util.Date;

/**
 * Attributes that uniquely identify a product.
 *
 * <dl>
 * <dt>Source</dt>
 * <dd>
 *   The organization <u>sending</u> the product;
 *   not necessarily the author of the product.
 * 
 *   Typically a FDSN network code.
 * </dd>
 * 
 * <dt>Type</dt>
 * <dd>
 *   The type of product being sent.
 * </dd>
 * 
 * <dt>Code</dt>
 * <dd>
 *   A unique code assigned by the <code>source</code> and <code>type</code>.
 *   Source and Type are effectively a namespace for codes.
 * 
 *   If the same <code>code</code> is re-used, it indicates a different
 *   version of the same product.
 * </dd>
 * 
 * <dt>Update Time</dt>
 * <dd>
 *   A timestamp representing when a product was created.
 * 
 *   Update Time is also used as a <strong>version</strong>.
 *   Products from the same <code>source</code> and <code>type</code> with
 *   the same <code>code</code> are considered different versions of the
 *   same product.
 * 
 *   More recent (newer) <code>updateTime</code>s
 *   supersede less recent (older) <code>updateTimes</code>.
 * </dd>
 * </dl>
 */
public class ProductId implements Comparable<ProductId> {

	/** Product source. */
	private String source;

	/** Product type. */
	private String type;

	/** Product code. */
	private String code;

	/** Product update time. */
	private Date updateTime;

	/**
	 * Create a new ProductId.
	 * 
	 * Same as new ProductId(type, code, source, new Date()).
	 */
	public ProductId(final String source, final String type, final String code) {
		this(source, type, code, new Date());
	}

	/**
	 * Create a new ProductId.
	 * 
	 * @param source
	 *            the product source.
	 * @param type
	 *            the product type.
	 * @param code
	 *            the product code.
	 * @param updateTime
	 *            when the product was updated.
	 */
	public ProductId(final String source, final String type, final String code,
			final Date updateTime) {
		setSource(source);
		setType(type);
		setCode(code);
		setUpdateTime(updateTime);
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source
	 *            the source to set
	 */
	public void setSource(String source) {
		this.source = escapeIdPart(source);
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type) {
		this.type = escapeIdPart(type);
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code
	 *            the code to set
	 */
	public void setCode(String code) {
		this.code = escapeIdPart(code);
	}

	/**
	 * @return the updateTime
	 */
	public Date getUpdateTime() {
		return updateTime;
	}

	/**
	 * @param updateTime
	 *            the updateTime to set
	 */
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	/**
	 * Convert this product id to a string. This string does not include the
	 * update time.
	 * 
	 * @return a product id string.
	 */
	public String toString() {
		return "urn:usgs-product:" + source + ":" + type + ":" + code + ":"
				+ Long.toString(updateTime.getTime());
	}

	/**
	 * Parse a product id string.
	 * 
	 * @param str
	 *            a valid product id string.
	 * @return a ProductId object.
	 */
	public static ProductId parse(final String str) {
		String[] parts = str.split(":");
		try {
			if (!"urn".equals(parts[0]) || !"usgs-product".equals(parts[1])) {
				throw new Exception("Expected product urn");
			}
			String source = parts[2];
			String type = parts[3];
			String code = parts[4];
			String updateTime = parts[5];
			return new ProductId(source, type, code, new Date(Long
					.valueOf(updateTime)));
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid ProductId '" + str + "'");
		}
	}

	/**
	 * Override the default Object.equals().
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj != null && obj instanceof ProductId) {
			if (compareTo((ProductId) obj) == 0) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Implement the Comparable interface.
	 * 
	 * @param that
	 *            product id being compared.
	 * @return -1 if this precedes that, 0 if same, and 1 if that precedes this.
	 */
	@Override
	public int compareTo(ProductId that) {
		int compare = getUpdateTime().compareTo(that.getUpdateTime());

		// same update time?
		if (compare == 0) {
			// to string includes source, type, code
			compare = toString().compareTo(that.toString());
		}

		return compare;
	}

	@Override
	public int hashCode() {
		return getSource().hashCode() + getType().hashCode()
				+ getCode().hashCode() + getUpdateTime().hashCode();
	}

	/**
	 * Whether these are the same product, even if they are different versions.
	 * 
	 * It is possible for isSameProduct to return true if equals returns false,
	 * but if equals returns true isSameProduct will also return true.
	 * 
	 * @param that
	 *            a ProductId to test.
	 * @return true if these are the same product (source,type,code), false
	 *         otherwise.
	 */
	public boolean isSameProduct(ProductId that) {
		if (getSource().equals(that.getSource())
				&& getType().equals(that.getType())
				&& getCode().equals(that.getCode())) {
			return true;
		}
		return false;
	}

	/**
	 * Escape id parts so they do not interfere with formatting/parsing.
	 * 
	 * @param part
	 *            part to escape.
	 * @return escaped part.
	 */
	private String escapeIdPart(final String part) {
		if (part == null) {
			return null;
		}

		String escaped = part;
		if (escaped.indexOf(":") != -1) {
			escaped = escaped.replace(":", "_");
		}
		return escaped;
	}
}
