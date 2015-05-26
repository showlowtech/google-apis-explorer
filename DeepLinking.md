**Table of Contents**


# Introduction #

If you're working with an API, you may occasionally find yourself wanting to send a link to a teammate to a particular method, to let them try it out.

The APIs Explorer makes this easy by augmenting the URL's fragment.

# Linking to a Service and Version #

To link to a particular version of a particular service, construct a link like so:

  * https://code.google.com/apis/explorer/
  * #`_`s=_{serviceName}_
  * #`_`v=_{version}_

For example, this link pre-selects v1 of the Buzz API:

```
https://code.google.com/apis/explorer/#_s=buzz&_v=v1
```

# Linking to a Method #

To drill deeper into a service, you can construct a link that pre-selects a given method of a service.

  * https://code.google.com/apis/explorer/
  * #`_`s=_{serviceName}_
  * #`_`v=_{version}_
  * #`_`m=_{methodName}_

This link pre-selects Buzz v1's `activities.search` method:

```
https://code.google.com/apis/explorer/#_s=buzz&_v=v1&_m=activities.search
```

# Pre-filling Parameter Values #

If you'd like to send a link that pre-fills some parameter values, that is also easy, just add the parameter names and values to the URL.

  * https://code.google.com/apis/explorer/
  * #`_`s=_{serviceName}_
  * #`_`v=_{version}_
  * #`_`m=_{methodName}_
  * #_{paramName1}_=_{paramValue1}_
  * #_{paramName2}_=_{paramValue2}_

This link pre-selects Buzz v1's `activities.search` method, and pre-fills the `q` and `max-results` parameters:

```
http://code.google.com/apis/explorer/#_s=buzz&_v=v1&_m=activities.search&alt=json&max-results=4&q=Google
```

Remember to URL-encode any parameter values when specifying parameters this way. This URL pre-fills the `q` parameter with a value containing spaces:

```
http://code.google.com/apis/explorer/#_s=buzz&_v=v1&_m=activities.search&max-results=4&q=Google%20APIs%20Explorer
```

# Additional Notes #

Note that there is currently no way to specify in a URL that Private Access should be requested, or to automatically execute the request. This is on purpose, since this might allow for malicious behavior.

For your convenience, whenever you execute a request, the address bar automatically updates to reflect the request that was just executed, so you can execute a request, copy the URL in the address, and send it to a teammate without having to construct this Explorer URL yourself.

This also allows you to go Back in the browser to load previously executed requests.