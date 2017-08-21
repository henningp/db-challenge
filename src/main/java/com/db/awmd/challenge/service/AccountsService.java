package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InsufficientFundsException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;

import static java.text.MessageFormat.format;

@Service
public class AccountsService {

  private final AccountsRepository accountsRepository;
  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void transfer(@NonNull String sourceAccountId, @NonNull String targetAccountId, @NonNull BigDecimal amount)
      throws InsufficientFundsException {
    final Account source = this.getExistingAccount(sourceAccountId);
    final Account target = this.getExistingAccount(targetAccountId);

    checkFundsAndTransfer(amount, source, target);
    sendNotifications(amount, source, target);
  }

  private void checkFundsAndTransfer(final BigDecimal amount, final Account source, final Account target) {
    Assert.isTrue(amount.compareTo(BigDecimal.ZERO) > 0,
        format("Transfer amount must be positive, was {0}. Source: {1}, target: {2}", amount, source, target));
    Assert.isTrue(source != target,
        format("Source and target must be different accounts, both were: {0}", source.getAccountId()));
    /*
      In a real system with some kind of persistent storage, we would most likely use transactions and/or optimistic
      locking at the service level to avoid race conditions and make the transfer atomic. Here, we can get away
      with 'synchronized'.

      Also, as receiving funds cannot fail, we don't need to worry about taking out money out of the source account and
      then not being able to put it into the target account.
     */
    synchronized (source) {
      if (!hasSufficientFunds(source, amount)) {
        throw new InsufficientFundsException(
            format("Account {0} does not have sufficient funds. Available: {1}, requested: {2}",
                source.getAccountId(), source.getBalance(), amount)
        );
      }

      source.setBalance(source.getBalance().subtract(amount));
    }
    synchronized (target) {
      target.setBalance(target.getBalance().add(amount));
    }
  }

  private void sendNotifications(BigDecimal amount, Account from, Account to) {
    notificationService.notifyAboutTransfer(
        from, format("You have sent {0} to account {1}.", amount, to.getAccountId()));
    notificationService.notifyAboutTransfer(
        to, format("You have received {0} from account {1}.", amount, from.getAccountId()));
  }

  private Account getExistingAccount(String accountId) {
    final Account result = this.accountsRepository.getAccount(accountId);
    Assert.notNull(result, format("Account {0} not found.", accountId));
    return result;
  }

  private boolean hasSufficientFunds(Account account, BigDecimal requiredFunds) {
    return account.getBalance().compareTo(requiredFunds) >= 0;
  }
}
