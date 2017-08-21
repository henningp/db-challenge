How to transfer:
----------------

    curl -X POST \
      http://localhost:18080/v1/accounts/id-1/transfer \
      -H 'content-type: application/json' \
      -d '{
    	"targetAccountId": "id-2",
    	"amount": 10
    }'

Implementation notes:
---------------------

1. In a real system with some kind of persistent storage, we would most likely use transactions 
and/or optimistic locking and/or retries at the service level to avoid race conditions and make 
the transfer atomic. In this non-persistent, non-distributed toy application, we can get away 
with `synchronized to solve the question of race conditions. As adding money to an account 
cannot fail, we also never have to lock two objects at the same time, hence there cannot be 
deadlocks which we then also don’t have to deal with (profit!).
2. I would tend to write controller and service tests as unit tests rather than integration 
tests, and use integration tests sparingly. This saves the trouble of having to pull up a Spring 
context for the tests, and having to substitute mocks or dummy objects for the actual 
implementations in the test context, which can become quite a burden in systems with more moving 
parts. I added fresh tests to `AccountsControllerTest.java` to save time, but created a separate 
Spock unit test for AccountsService (`AccountsServiceSpec.groovy`) to illustrate this. (I left the 
integration tests for the existing methods in place.) It would also make sense to be able to run 
fast unit tests and slower integration tests separately.
3. If I go to the lengths of creating a custom exception class, I might also advertise it at the 
service level even though it is unchecked (`… myMethod(…) throws MyUncheckedException {…}`). IDEs 
have finally stopped complaining about this.


Architectural ideas:
--------------------

1. In an account, the current balance is the result of all the transactions of the past, which, in a 
real system, would need to be stored somewhere anyway. That sounds like a good fit for an event 
sourcing approach.
2. The different roles an account can play (as source or target account) could also be a good fit for 
the DCI pattern, which would allow breaking out of the procedural programming style typical for 
request-response applications. (For this toy application, it would be complete overkill, of course.)