package com.cayman.web.account;

import com.cayman.dto.TransactionTransferObject;
import com.cayman.entity.Account;
import com.cayman.entity.Currency;
import com.cayman.util.AccountUtil;
import com.cayman.util.TimeUtil;
import com.cayman.util.exceptions.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@Controller
@RequestMapping(value = "/accounts")
public class JspAccountController extends AbstractAccountController {

    @RequestMapping(method = RequestMethod.GET)
    public String accountList(Model model) {
        model.addAttribute("accountList", super.getAll());
        return "accountList";
    }

    @RequestMapping(value = "/menu", method = RequestMethod.GET)
    public String editForUpdate(HttpServletRequest request, Model model){
        model.addAttribute("account", super.get(getId(request)));
        return "accountEdit";
    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String editForCreate(Model model) {
        model.addAttribute("account", new Account("My New Account"));
        return "accountCreate";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String updateOrCreate(HttpServletRequest request) {
        String id = request.getParameter("id");
        String accountNumber = request.getParameter("accountNumber");
        String currency = request.getParameter("currency");
        Account account = new Account(id.isEmpty() ? null : Integer.parseInt(id),
                request.getParameter("name"),
                accountNumber.isEmpty() ? null : accountNumber,
                Currency.valueOf(currency),
                AccountUtil.createBigDecimal(Double.parseDouble(request.getParameter("balance"))));
        if (account.isNew()) {
            super.create(account);
        } else {
            super.update(account);
        }
        return "redirect:/accounts";
    }

    @RequestMapping(value = "menu/delete", method = RequestMethod.GET)
    public String delete(HttpServletRequest request){
        try {
            super.delete(getId(request));
        } catch (CannotDeleteEntityException e) {
            return "errors/cannotDeleteAccount";
        }
        return "redirect:/accounts";
    }

    /*@RequestMapping(value = "/putMoney", method = RequestMethod.POST)
    public String putMoney(HttpServletRequest request) {
        int id = getId(request);
        BigDecimal value = AccountUtil.createBigDecimal(Double.parseDouble(request.getParameter("value")));
        return "";
    }*/

    @RequestMapping(value = "/getForAdding", method = RequestMethod.GET)
    public String getAccountForAddingMoney(HttpServletRequest request, Model model) {
        model.addAttribute("account", super.get(getId(request)));
        return "putMoney";
    }

    @RequestMapping(value = "/getForSend", method = RequestMethod.GET)
    public String getForSend(HttpServletRequest request, Model model) {
        model.addAttribute("account", super.get(getId(request)));
        return "createTransaction";
    }

    @RequestMapping(value= "/putMoney", method = RequestMethod.POST)
    public String putMoneyIntoAccount(HttpServletRequest request){
        try {
            super.putMoney(getId(request), AccountUtil.createBigDecimal(Double.parseDouble(request.getParameter("value"))));
        } catch (NotAvailableAccountException e) {
            return "errors/accountIsBlocked";
        } catch (IncorrectAmountException e) {
            return "errors/incorrectTransferAmount";
        } catch (NumberFormatException e) {
            return "errors/incorrectTransferAmount";
        }
        return "redirect:/accounts";
    }

    @RequestMapping(value = "/createTransaction", method = RequestMethod.POST)
    public String getTransactionInformation(HttpServletRequest request, Model model) {
        TransactionTransferObject dto;
        try {
            dto = super.getTransactionInformation(
                    getId(request),
                    request.getParameter("accountNumber"),
                    request.getParameter("comment"),
                    AccountUtil.createBigDecimal(Double.parseDouble(request.getParameter("value"))));
        } catch (NotFoundEntityException e){
            return "errors/accountIsNotExist";
        } catch (NotEnoughMoneyInTheAccountException e){
            return "errors/notEnoughMoney";
        } catch (NotAvailableAccountException e) {
            return "errors/accountIsBlocked";
        } catch (IncorrectAmountException e) {
            return "errors/incorrectTransferAmount";
        } catch (NumberFormatException e) {
            return "errors/incorrectTransferAmount";
        }
        model.addAttribute("dto", dto);
        return "transactionInformation";
    }

    @RequestMapping(value = "/sendMoney", method = RequestMethod.POST)
    public String sendMoney(HttpServletRequest request){
        try {
            super.sendMoney(
                    Integer.parseInt(request.getParameter("senderId")),
                    Integer.parseInt(request.getParameter("senderAccountId")),
                    Integer.parseInt(request.getParameter("recipientId")),
                    Integer.parseInt(request.getParameter("recipientAccountId")),
                    Currency.valueOf(request.getParameter("commissionCurrency")),
                    request.getParameter("comment"),
                    AccountUtil.createBigDecimal(Double.parseDouble(request.getParameter("transferAmount"))),
                    AccountUtil.createBigDecimal(Double.parseDouble(request.getParameter("commission"))),
                    AccountUtil.createBigDecimal(Double.parseDouble(request.getParameter("amountForReceive"))));
        } catch (NotAvailableAccountException e) {
            return "errors/accountIsBlocked";
        }
        return "redirect:/accounts";
    }

    @RequestMapping(value = "/history", method = RequestMethod.GET)
    public String getAccountHistory(HttpServletRequest request, Model model) {
        int id = getId(request);
        model.addAttribute("accountHistory", super.getAccountHistory(id));
        model.addAttribute("id", id);
        return "accountHistory";
    }

    @RequestMapping(value = "/history/filter", method = RequestMethod.POST)
    public String getFilteredHistory(HttpServletRequest request, Model model) {
        LocalDate startDate = TimeUtil.parseLocalDate(resetParam("startDate", request), TimeUtil.MIN_DATE);
        LocalDate endDate = TimeUtil.parseLocalDate(resetParam("endDate", request), TimeUtil.MAX_DATE);
        LocalTime startTime = TimeUtil.parseLocalTime(resetParam("startTime", request), LocalTime.MIN);
        LocalTime endTime = TimeUtil.parseLocalTime(resetParam("endTime", request), LocalTime.MAX);
        String option = request.getParameter("option");
        int accountId = getId(request);
        model.addAttribute("accountHistory", super.getHistoryBetweenWithOption(
                startDate, startTime,
                endDate, endTime,
                accountId, option));
        model.addAttribute("id", accountId);
        return "accountHistory";
    }

    private String resetParam(String param, HttpServletRequest request) {
        String value = request.getParameter(param);
        request.setAttribute(param, value);
        return value;
    }

    private int getId(HttpServletRequest request) {
        String id = Objects.requireNonNull(request.getParameter("id"));
        return Integer.parseInt(id);
    }
}
