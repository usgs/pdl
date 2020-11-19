# AWS Hub

The AWS hub uses HTTPS and can be accessed from any language.

## Notification API

The notification API uses a persistent web socket connection to notify clients
when products are sent. Messages use JSON format and include an **action** key
that describes the message.

### Notifications

Notifications have a **created** attribute when a product was sent
(with microsecond precision), and a **product** attribute with product information.

```
{
  "created": <ISO8601>,
  "product": <Product>
}
```

### Broadcast

After a client connects, they will receive **broadcast** messages for any
products sent while they are connected.

```
{
  "action": "broadcast",
  "notification": <Notification>
}
```

### Reliable Processing

Clients that wish to process all products should track the _created_ timestamp
of the last notification they received, and use the following process when
connecting:

1. ignore _broadcast_ messages until caught up, but keep track of last received
   broadcast (to check when caught up).

2. sent a **products_created_after** request after connecting:

   ```
   {
     "action": "products_created_after",
     "created_after": <ISO8601(created)>
   }
   ```

3. receive zero or more **product** messages in response to the
   _products_created_after_ request. Each product should be processed,
   and the _created_ timestamp being tracked updated to this new value.

   ```
   {
     "action": "product",
     "notification": <Notification>
   }
   ```

4. receive a **products_created_after** response, indicating no more _product_
   messages will be sent:

   ```
   {
     "action": "products_created_after",
     "created_after": <ISO8601(created)>,
     "count": <number of product messages that were sent>
   }
   ```

5. check whether caught up:

- if a _broadcast_ message has been received:
  if the most recent _notification_ _created_ timestamp
  is not before the most recent _broadcast_ _created_ timestamp,
  **switch to processing broadcasts**.

- if no _broadcast_ message has been received:
  if the _count_ of products sent is zero,
  **switch to processing broadcasts**.

- otherwise, **return to step 2** and send another _products_created_after_
  request until caught up.

## Send API

### Get Upload Urls

This step is optional for products that do not have any contents or
only have empty path content.

#### Request

> URL values within the product are not used,
> and can be set to null in the request.

```
POST https://<HUBURL>/get_upload_urls HTTP/1.1
Content-Type: application/json

{
  "product": <Product>
}
```

Product signature is verified before further processing, and returns a
HTTP status code `401` if the signature is invalid.

#### Response

```
{
  "already_exists": <Boolean>,
  "product": <Product>
}
```

`already_exists` is set to `true` if the product has already been sent.
Otherwise, the `product` is returned with content urls updated to be signed urls
that can be used to upload contents directly to S3.

### Upload Content

> Note that empty path contents should not be uploaded,
> as this content doesn't correspond to a file.

The URLs returned by the Get Upload Urls step are signed and must include the
following headers:

```
PUT <SignedUrl> HTTP/1.1
Content-Length: <Length>
Content-Type: <Type>
x-amz-meta-sha256: <Sha256>
x-amz-meta-modified: <ISO8601(Modified, millisecond precision)>

<Bytes>
```

The HTTP response should be `200 OK` if upload succeeded.

### Send Product

Once contents have been uploaded, the product is sent with another request:

#### Request

```
POST https://<HUBURL>/send_product
Content-Type: application/json

{
  "product": <Product>
}
```

#### Response

```
{
  "already_exists": <Boolean>
  "notification": <Notification>
  "sns_message_id": <String>
}
```

## JSON Formatting

### ISO8601

Dates are formatted as ISO8601 strings. These currently support microsecond
precision (6 decimals for fractional seconds), but product information uses
millisecond precision for backward compatibility.

```
YYYY-MM-DDTHH:MM:SS.SSSSSSZ
2020-10-21T16:53:14.123456Z
```

> Note the **`T`** separator between date and time and the **`Z`** UTC timezone
> suffix, which are both required.

### Product

```
{
  "contents": [
    {
      "length": 157,
      "modified": "2020-10-13T15:05:03.000Z",
      "path": "",
      "sha256": "hkomWf290QUmWGV7jOfxhqOu8kS45NglYAQkIUKOyGI=",
      "type": "text/plain",
      "url": "data:text/plain;base64,OyBFTU06IHNlbmRIZWFydGJlYXQuZXJiIHVwZGF0ZWQgMjAxOC0wOS0wNQpoZWFydGJlYXRfaGVhZF91cmw9J2h0dHBzOi8vZ2l0aHViLmNvbS91c2dzL3BkbC9ldGMvZXhhbXBsZXMvaGVhcnRiZWF0L3NlbmRIZWFydGJlYXQnOwpoZWFydGJlYXRfcGRsX3ZlcnNpb249Jyc7Cg=="
    }
  ],
  "geometry":null,
  "id": {
    "code":"lenore",
    "source": "heartbeat-source",
    "type":"heartbeat",
    "updateTime":"2020-10-13T15:05:03.140Z"
  },
  "links":[],
  "properties":{
    "original-signature-version":"v1",
    "original-signature":"MCwCFGG5LqIZs8eGeG1uJPW8g+WFBq2oAhRsoFvjlm6V6UFJbH5ywVjrda1b5w==",
    "pdl-client-version":"Version 2.5.1 2020-06-25"
  },
  "signature":"MC0CFQCM8rouUxcd8DY/vdwtwgGYmZdQFAIUCj23Fm+x0uk4XJ/OOmV2m733tHU=",
  "signatureVersion":"v1",
  "status":"UPDATE",
  "type":"Feature"
}

```

```

```

```

```
