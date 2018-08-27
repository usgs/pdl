/**
 * Classes that perform input and output for Products.
 *
 * The two primary interfaces are ProductSource and ProductHandler.
 * A ProductSource generates product events.
 * A ProductHandler handles product events.
 *
 * The idea behind these interfaces is to process products using streams,
 * to minimize the memory footprint and to allow creation and processing
 * of products with contents that cannot fit within a virtual machine.
 *
 * There are four main ProductSource/ProductHandler pairs:
 * <dl>
 * <dt>Object</dt>
 * <dd>
 * Java Object =&gt; ObjectProductSource =&gt; events<br>
 * events =&gt; ObjectProductHandler =&gt; Java Object<br>
 * </dd>
 *
 * <dt>Directory</dt>
 * <dd>
 * product directory =&gt; DirectoryProductSource =&gt; events<br>
 * events =&gt; DirectoryProductHandler =&gt; product directory
 * </dd>
 *
 * <dt>Xml</dt>
 * <dd>
 * product xml =&gt; XmlProductSource =&gt; events<br>
 * events =&gt; XmlProductHandler =&gt; product xml<br>
 * </dd>
 *
 * <dt>Zip</dt>
 * <dd>
 * product zip =&gt; ZipProductSource =&gt; events<br>
 * events =&gt; ZipProductHandler =&gt; product zip<br>
 * </dd>
 * </dl>
 */
package gov.usgs.earthquake.product.io;
