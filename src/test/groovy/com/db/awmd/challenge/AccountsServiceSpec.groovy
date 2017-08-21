package com.db.awmd.challenge

import com.db.awmd.challenge.domain.Account
import com.db.awmd.challenge.exception.InsufficientFundsException
import com.db.awmd.challenge.repository.AccountsRepository
import com.db.awmd.challenge.service.AccountsService
import com.db.awmd.challenge.service.NotificationService
import spock.lang.Specification
import spock.lang.Unroll

class AccountsServiceSpec extends Specification {

    def sourceAccount = new Account('source-id', 123.45)
    def targetAccount = new Account('target-id', 0)

    def repo = Mock(AccountsRepository) {
        getAccount('source-id') >> sourceAccount
        getAccount('target-id') >> targetAccount
    }
    def notificationService = Mock(NotificationService)

    def underTest = new AccountsService(repo, notificationService)

    @Unroll
    def "transfer checks arguments for null"() {
        when:
        underTest.transfer(source, target, amount)

        then:
        thrown(NullPointerException)

        where:
        source          | target        | amount
        null            | 'target-id'   | 12.34
        'source-id'     | null          | 12.34
        'source-id'     | 'target-id'   | null
    }

    @Unroll
    def "transfer throws IllegalArgumentException when amount is non-positive"() {
        when:
        underTest.transfer('source-id', 'target-id', nonPositiveAmout)

        then:
        thrown(IllegalArgumentException)

        where:
        nonPositiveAmout << [0, -1, -1000]
    }

    def "transfer throws IllegalArgumentException when source or target account does not exist"() {
        when:
        underTest.transfer(source, target, 12.34)

        then:
        thrown(IllegalArgumentException)

        where:
        source          | target
        'nonexistent'   | 'target-id'
        'source-id'     | 'nonexistent'
    }

    def "transfer throws IllegalArgumentException when source and target are identical"() {
        when:
        underTest.transfer('source-id', 'source-id', 12.34)

        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "transfer throws InsufficientFundsException when the source account does not contain at least the amount"() {
        when:
        underTest.transfer('source-id', 'target-id', tooMuch)

        then:
        thrown(InsufficientFundsException)

        where:
        tooMuch << [123.46, 1000.00]
    }

    def "transfer moves funds from source to target account"() {
        when:
        underTest.transfer('source-id', 'target-id', 10.00)

        then:
        sourceAccount.balance == 113.45
        targetAccount.balance == 10.00
    }

    def "transfer can completely empty source account"() {
        when:
        underTest.transfer('source-id', 'target-id', 123.45)

        then:
        sourceAccount.balance == 0.00
        targetAccount.balance == 123.45
    }

    def "transfer sends notifications to source and target account holders containing account ID and amount"() {
        when:
        underTest.transfer('source-id', 'target-id', 10.00)

        then:
        1 * notificationService.notifyAboutTransfer(
                sourceAccount, { it.contains('target-id') && it.contains('10') } as String)
        1 * notificationService.notifyAboutTransfer(
                targetAccount, { it.contains('source-id') && it.contains('10') } as String)
    }

    def "transfer does not send notifications on failure"() {
        when:
        underTest.transfer('source-id', 'target-id', 1000.00)

        then:
        thrown(InsufficientFundsException)
        0 * notificationService._
    }

}
