package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private AccountsRepository accountsRepository;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsRepository.clearAccounts();
  }

  @Test
  public void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void getAccount() throws Exception {
    String uniqueAccountId = makeAccount();

    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  public void transfer_shouldReturnHttp400_whenNoBodyIsSent() throws Exception {
    String someAccountId = makeAccount();

    this.mockMvc.perform(post("/v1/accounts/"+ someAccountId + "/transfer")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void transfer_shouldReturnHttp400_whenAmountIsNegative() throws Exception {
    String someAccountId = makeAccount();

    this.mockMvc.perform(post("/v1/accounts/"+ someAccountId + "/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"toAccountId\":\"some-other-account\",\"amount\":-1000}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void transfer_shouldReturnHttp400_whenAmountIsZero() throws Exception {
    String someAccountId = makeAccount();

    this.mockMvc.perform(post("/v1/accounts/"+ someAccountId + "/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"targetAccountId\":\"some-other-account\",\"amount\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void transfer_shouldReturnHttp400_whenTargetAccountIdIsBlank() throws Exception {
    String someAccountId = makeAccount();

    this.mockMvc.perform(post("/v1/accounts/"+ someAccountId + "/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"targetAccountId\":\"\",\"amount\":1000}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void transfer_shouldReturnHttp400_whenSourceAccountHasInsufficientFunds() throws Exception {
    String sourceAccountId = makeAccount();
    String targetAccountId = makeAccount();

    this.mockMvc.perform(post("/v1/accounts/"+ sourceAccountId + "/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"targetAccountId\":\""+targetAccountId+"\",\"amount\":1000}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void transfer_shouldPerformTransfer_whenAllArgumentsAreValid() throws Exception {
    String sourceAccountId = makeAccount();
    String targetAccountId = makeAccount();

    this.mockMvc.perform(post("/v1/accounts/"+ sourceAccountId + "/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"targetAccountId\":\""+targetAccountId+"\",\"amount\":23.45}"))
        .andExpect(status().isOk());

    assertThat(this.accountsService.getAccount(sourceAccountId).getBalance()).isEqualByComparingTo("100");
    assertThat(this.accountsService.getAccount(targetAccountId).getBalance()).isEqualByComparingTo("146.90");
  }

  private String makeAccount() {
    String uniqueAccountId = "Id-" + UUID.randomUUID();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    return uniqueAccountId;
  }



}
