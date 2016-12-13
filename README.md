# tc-passthrough-testing
Enables simpler functional testing of Terracotta entities via pass-through communication.


Uses of Passthrough
-----

The benefit of this implementation of the Terracotta API is that is allows development and basic testing of entities and services, purely in-process.

This dramatically shortens the development cycle when initially building an entity or service since a full packaging and deployment is not required.

Additionally, this allows debugging problems in a single process instead of across several.


Limits of Passthrough
-----

The passthrough server is purely single-threaded, meaning that it can't be used to test concurrent execution within entities.

In the case of any behavioral difference between passthrough and the reference implementation of the server (found in terracotta-core repository), the reference implementation is considered the more correct interpretation.

Passthrough does not read Terracotta server config files but must be manually configured.

Passthrough does not isolate entity types or services within their own classloaders.


Concerning bugs found in Passthrough
-----

Before opening a bug against passthrough, ensure that the test in question does work as expected on the reference implementation.

Even in the case where there is a difference in behavior between passthrough and reference, it may not be feasible/valuable to fix it.  While this tool provides value, it has no explicit support.

In the cases where API-defined contract is violated or substantial concepts are improperly handled, this is likely to be fixed in passthrough.  In the cases of subtle behavioral differences or limits of its capabilities (such as the aforementioned single-threaded nature), the bug will likely be closed with only clarification as to the reason why the problem exists.
