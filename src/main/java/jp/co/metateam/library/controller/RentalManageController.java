package jp.co.metateam.library.controller;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
 
import jp.co.metateam.library.service.AccountService;
import jp.co.metateam.library.service.RentalManageService;
import jp.co.metateam.library.service.StockService;
import lombok.extern.log4j.Log4j2;
import jp.co.metateam.library.model.RentalManage;
 
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import jp.co.metateam.library.model.RentalManageDto;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import jp.co.metateam.library.values.RentalStatus;
import jp.co.metateam.library.model.Stock;
import jp.co.metateam.library.model.Account;
import java.util.Date;
 
 
/**
 * 貸出管理関連クラスß
 */
@Log4j2
@Controller
public class RentalManageController {
 
    private final AccountService accountService;
    private final RentalManageService rentalManageService;
    private final StockService stockService;
 
    @Autowired
    public RentalManageController(
        AccountService accountService,
        RentalManageService rentalManageService,
        StockService stockService
    ) {
        this.accountService = accountService;
        this.rentalManageService = rentalManageService;
        this.stockService = stockService;
    }
 
    /**
     * 貸出一覧画面初期表示
     * @param model
     * @return
     */
    @GetMapping("/rental/index")
    public String index(Model model) {
        // 貸出管理テーブルから全件取得
 
        List <RentalManage> rentalManageList = this.rentalManageService.findAll();
       
        // 貸出一覧画面に渡すデータをmodelに追加
 
        model.addAttribute("rentalManageList", rentalManageList);
 
        // 貸出一覧画面に遷移
 
        return "rental/index";
    }
 
    @GetMapping("/rental/add")
    public String add(Model model) {
        List <Stock> stockList = this.stockService.findAll();
        List <Account> accounts = this.accountService.findAll();
 
        model.addAttribute("accounts", accounts);
        model.addAttribute("stockList",stockList);
        model.addAttribute("rentalStatus", RentalStatus.values());
 
        if (!model.containsAttribute("rentalManageDto")) {
            model.addAttribute("rentalManageDto", new RentalManageDto());
        }
 
        return "rental/add";
    }
 
 
    @PostMapping("/rental/add")
    public String save(@Valid @ModelAttribute RentalManageDto rentalManageDto, BindingResult result, RedirectAttributes ra) {
        try {
            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }
            // 登録処理
            this.rentalManageService.save(rentalManageDto);
 
            return "redirect:/rental/index";
        } catch (Exception e) {
            log.error(e.getMessage());
 
            ra.addFlashAttribute("rentalManageDto", rentalManageDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.rentalManageDto", result);
 
            return "redirect:/rental/add";
        }
    }
 
   
    @GetMapping("/rental/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        List <Stock> stockList = this.stockService.findAll();  //在庫管理番号のプルダウンリスト作成
        List <Account> accounts = this.accountService.findAll(); //社員番号のプルダウンリスト作成
     
            model.addAttribute("stockList", stockList); //在庫管理番号のリストを表示（プルダウン）
            model.addAttribute("accounts", accounts);  //社員番号のリストを表示（プルダウン）
            model.addAttribute("rentalStatus", RentalStatus.values());  //貸出ステータスリスト（プルダウン）
     
            RentalManage rentalManage = this.rentalManageService.findById(id); //貸出管理テーブルから{id}の情報を持ってくる
     
            /*
             * 取得した貸出管理情報をそれぞれセットする
             */
            if (!model.containsAttribute("rentalManageDto")) {
                RentalManageDto rentalManageDto = new RentalManageDto();
     
            rentalManageDto.setId(rentalManage.getId());
            rentalManageDto.setStatus(rentalManage.getStatus());
            rentalManageDto.setExpectedRentalOn(rentalManage.getExpectedRentalOn());
            rentalManageDto.setExpectedReturnOn(rentalManage.getExpectedReturnOn());
            rentalManageDto.setStockId(rentalManage.getStock().getId());
            rentalManageDto.setEmployeeId(rentalManage.getAccount().getEmployeeId());
     
            /*
             * セットした内容の表示
             */
            model.addAttribute("rentalManageDto", rentalManageDto);
        }
     
        return "rental/edit";
     }

     @PostMapping("/rental/{id}/edit")
    public String update(@PathVariable("id") String id, @Valid @ModelAttribute RentalManageDto rentalManageDto, BindingResult result, Model model){
 
        Integer afterStatus = rentalManageDto.getStatus();
        Date afterexpectedRentalOn = rentalManageDto.getExpectedRentalOn();
       // Date afterexpectedRentalOn = rentalManageDto.getExpectedRentalOn();
        LocalDate newexpectedRentalOn = afterexpectedRentalOn.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
         //今日の日付の取得
        LocalDate currentDate = LocalDate.now();
       
       
         try {
              RentalManage rentalManage = this.rentalManageService.findById(Long.valueOf(id));
              String validerror = rentalManageService.isStatusError(rentalManage.getStatus(), afterStatus, newexpectedRentalOn, currentDate);
 
           
             if (validerror != null) {
                result.addError(new FieldError("rentalManageDto","status",validerror));                
            }
             
             if (result.hasErrors()) {
                 throw new Exception("Validation error.");                
             }
             // 更新処理
             this.rentalManageService.update(Long.valueOf(id), rentalManageDto);
 
             return "redirect:/rental/index";
         } catch (Exception e) {
            log.error(e.getMessage());
            List <Stock> stockList = this.stockService.findAll();  //在庫管理番号のプルダウンリスト作成
            List <Account> accounts = this.accountService.findAll(); //社員番号のプルダウンリスト作成
       
                model.addAttribute("stockList", stockList); //在庫管理番号のリストを表示（プルダウン）
                model.addAttribute("accounts", accounts);  //社員番号のリストを表示（プルダウン）
                model.addAttribute("rentalStatus", RentalStatus.values());  //貸出ステータスリスト（プルダウン）
       
            return "rental/edit";//どのテンプレートをもってくるか
        }
     }
}
