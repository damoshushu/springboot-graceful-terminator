# SpringBoot Graceful Terminator
## Still in review. Will be added to public maven repository after final tests
Graceful Shutdown Logic for SpringBoot

Advanced SpringBoot Application.
Handles the (OpenShift) SIGTERM signal to
1. Let the running requests finish (within defined timeout)
2. Reject new Requests with HTTP 503 (to let loadbalancers forward the requests to other instances)
3. Stop Embedded Servers
4. Shutdown Application Context
