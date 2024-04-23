# Policy Definition

Old

```json
{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    },
    "@type": "PolicyDefinitionRequestDto",
    "@id": "{{POLICY_ID}}",
    "policy": {
        "@type": "Policy",
        "odrl:permission": [
            {
                "odrl:action": "USE",
                "odrl:constraint": {
                    "@type": "LogicalConstraint",
                    "odrl:or": [
                        {
                            "@type": "Constraint",
                            "odrl:leftOperand": "BusinessPartnerNumber",
                            "odrl:operator": {
                                "@id": "odrl:eq"
                            },
                            "odrl:rightOperand": "{{SUPPLIER_BPNL}}"
                        }
                    ]
                }
            }
        ]
    }
}
```

error

```json
[
    {
        "message": "https://w3id.org/edc/v0.0.1/ns/policy/@type was expected to be http://www.w3.org/ns/odrl/2/Set but it was not",
        "type": "ValidationFailure",
        "path": "https://w3id.org/edc/v0.0.1/ns/policy/@type",
        "invalidValue": [
            "https://w3id.org/edc/v0.0.1/ns/Policy"
        ]
    }
]
```

policy.@type = "odrl:Set"

catalog request results in unexpected null pinter during membershipPresentation call

SEVERE 2024-04-18T18:14:41.467647124 JerseyExtension: Unexpected exception caught
java.lang.NullPointerException
at java.base/java.util.Objects.requireNonNull(Unknown Source)
at java.base/java.util.ImmutableCollections$MapN.<init>(Unknown Source)
at java.base/java.util.Map.of(Unknown Source)
at org.eclipse.tractusx.edc.identity.mapper.BdrsClientImpl.createMembershipPresentation(BdrsClientImpl.java:153)
at org.eclipse.tractusx.edc.identity.mapper.BdrsClientImpl.updateCache(BdrsClientImpl.java:121)
at org.eclipse.tractusx.edc.identity.mapper.BdrsClientImpl.resolve(BdrsClientImpl.java:101)
at org.eclipse.tractusx.edc.identity.mapper.BdrsClientAudienceMapper.resolve(BdrsClientAudienceMapper.java:39)
at org.eclipse.edc.protocol.dsp.http.dispatcher.DspHttpRemoteMessageDispatcherImpl.dispatch(
DspHttpRemoteMessageDispatcherImpl.java:121)
at org.eclipse.edc.connector.core.message.RemoteMessageDispatcherRegistryImpl.dispatch(
RemoteMessageDispatcherRegistryImpl.java:48)
at org.eclipse.edc.connector.controlplane.services.catalog.CatalogServiceImpl.requestCatalog(CatalogServiceImpl.java:44)
at org.eclipse.edc.connector.controlplane.api.management.catalog.CatalogApiController.requestCatalog(
CatalogApiController.java:64)
at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Unknown Source)
at java.base/java.lang.reflect.Method.invoke(Unknown Source)
at org.glassfish.jersey.server.model.internal.ResourceMethodInvocationHandlerFactory.lambda$static$0(
ResourceMethodInvocationHandlerFactory.java:52)
at org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher$1.run(
AbstractJavaResourceMethodDispatcher.java:146)
at org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.invoke(
AbstractJavaResourceMethodDispatcher.java:189)
at org.glassfish.jersey.server.model.internal.JavaResourceMethodDispatcherProvider$VoidOutInvoker.doDispatch(
JavaResourceMethodDispatcherProvider.java:159)
at org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.dispatch(
AbstractJavaResourceMethodDispatcher.java:93)
at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:478)
at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:400)
at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:81)
at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:261)
at org.glassfish.jersey.internal.Errors$1.call(Errors.java:248)
at org.glassfish.jersey.internal.Errors$1.call(Errors.java:244)
at org.glassfish.jersey.internal.Errors.process(Errors.java:292)
at org.glassfish.jersey.internal.Errors.process(Errors.java:274)
at org.glassfish.jersey.internal.Errors.process(Errors.java:244)
at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:265)
at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:240)
at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:697)
at org.glassfish.jersey.servlet.WebComponent.serviceImpl(WebComponent.java:394)
at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:346)
at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:357)
at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:311)
at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:205)
at org.eclipse.jetty.servlet.ServletHolder.handle(ServletHolder.java:764)
at org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:529)
at org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:221)
at org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1381)
at org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:176)
at org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:484)
at org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:174)
at org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1303)
at org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:129)
at org.eclipse.jetty.server.handler.ContextHandlerCollection.handle(ContextHandlerCollection.java:192)
at org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:122)
at org.eclipse.jetty.server.Server.handle(Server.java:563)
at org.eclipse.jetty.server.HttpChannel$RequestDispatchable.dispatch(HttpChannel.java:1598)
at org.eclipse.jetty.server.HttpChannel.dispatch(HttpChannel.java:753)
at org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:501)
at org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:287)
at org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:314)
at org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:100)
at org.eclipse.jetty.io.SelectableChannelEndPoint$1.run(SelectableChannelEndPoint.java:53)
at org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:969)
at org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.doRunJob(QueuedThreadPool.java:1194)
at org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.run(QueuedThreadPool.java:1149)
at java.base/java.lang.Thread.run(Unknown Source)

--> bpdrs service needed

# bdrs

Calls needed:

- management -> create bpn directory
- BPN-Directory -> map of bpn and did reachable

bpn directory is securied by jwt containing:

- vp holder key
- vp holder id
- null
- bdrs-server-audience
- map with
    - vp following content example with holderId and vcJwt

vcJwt inserted is build with

- spoofedKey
- issuerId
- degreeSub
- holderId
- map with
    - vc = membership_credential with holderId inserted

DIDs are build following JsonWebKey2020
DID-ID like did:web:name-to-use

Credential Service
mock: https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-extensions/bdrs-client/src/test/java/org/eclipse/tractusx/edc/identity/mapper/BdrsClientImplComponentTest.java

Update Cache -> why does it need a bearer token with the membershipCredToken sent to /bpn-directory?
https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-extensions/bdrs-client/src/main/java/org/eclipse/tractusx/edc/identity/mapper/BdrsClientImpl.java#L92

Dids seem to
be [dependent on the hosting companies' url](https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/tx.dataspace.topology.md)

Seems like:

- MIW / DIM are credential services
- Portal + DIM are issuer services

A client uses a token during a request, to grant access to specific resources

- verifier uses it to request the vp
- the CS endpoint is resolved using bdrs
- **What's the bearer access scope**

access scopes

- org.eclipse.tractusx.vc.type:Member:read
- org.eclipse.tractusx.vc.id:uuid:read -> give access to verifieable credential by id

Endoints:

- POST presentations/query
    - uses OAuth2 scopes that need to be mapped to presentation definition
- storage api credentials

https://github.com/eclipse-edc/Connector/blob/4fd16b8e34d685239ea40fc3d8e9b02cc8ccf323/core/common/token-core/src/main/java/org/eclipse/edc/token/TokenValidationServiceImpl.java#L54

- a key is somehow resolved. This may be from the did.json

bdrs | WARNING 2024-04-21T19:34:23.497508523 Error validating BDRS client VP: Credential is not yet valid., Not all
credential subject IDs match the expected subject ID 'did:web:edr-service/trusted-issuer'. Violating subject
IDs: [did:web:edr-service/supplier]

- I need to check on subjects - likely this is wrong
- not sure why the credential should not be valid

Following
this [test](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-extensions/bdrs-client/src/test/java/org/eclipse/tractusx/edc/identity/mapper/BdrsClientImplComponentTest.java),

- the VC is signed by the issuer
- the VP is signed by the holder

Why does the catalog request on consumer side result in port 443 / ssl?

Overall request is the dim request
-> check in dim test

Following Tractus-X Connector Setup

- DIM = your wallet that already contains VCs
- Credential Service = get your own VP to hand over. (something like the miw)
- SecureTokenService = get auth for something and then request presentation

customer-control-plane | DEBUG 2024-04-22T17:49:36.200763912 DSP: Incoming CatalogRequestMessage for class
org.eclipse.edc.connector.controlplane.catalog.spi.Catalog process
customer-control-plane | DEBUG 2024-04-22T17:49:36.267334896 Unauthorized: No Service endpoint 'CredentialService' found
on DID Document.
customer-control-plane | DEBUG 2024-04-22T17:49:36.267812038 DSP: Service call failed: Unauthorized
