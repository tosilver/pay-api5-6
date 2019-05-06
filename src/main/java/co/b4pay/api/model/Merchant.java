package co.b4pay.api.model;

import co.b4pay.api.model.base.BaseEntity;
import org.hibernate.annotations.Proxy;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * 商户信息
 *
 * @author YK
 * @version $Id: Merchant.java, v 0.1 2018年4月20日 下午14:03:09 YK Exp $
 */
@Entity
@Table(name = "dst_merchant")
@Proxy(lazy = false)
public class Merchant extends BaseEntity {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -3050115515232663085L;

    /**
     * 实体编号（唯一标识）
     */
    @Id
    private Long id;                                     // 唯一标识ID
    private String company;                                // 公司名
    private String contacts;                               // 联系人
    private String idCard;                                 // 身份证号
    private String tel;                                    // 联系电话
    private Long provinceId;                             // 省份
    private Long cityId;                                 // 城市
    private Long areaId;                                 // 地区
    private String address;                                // 公司地址
    private String secretKey;                              // 密钥
    private Integer status;
    private BigDecimal balance;                            //账户总余额
    private BigDecimal accountBalance;                       //入账余额
    private BigDecimal account_frozen;                      //出账冻结金额
    private BigDecimal withdrawBalance;                      //可提现金额
    private BigDecimal withdrawFrozen;                      //提现冻结金额

    public BigDecimal getAccount_frozen() {
        return account_frozen;
    }

    public void setAccount_frozen(BigDecimal account_frozen) {
        this.account_frozen = account_frozen;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getContacts() {
        return contacts;
    }

    public void setContacts(String contacts) {
        this.contacts = contacts;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public Long getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(Long provinceId) {
        this.provinceId = provinceId;
    }

    public Long getCityId() {
        return cityId;
    }

    public void setCityId(Long cityId) {
        this.cityId = cityId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(BigDecimal accountBalance) {
        this.accountBalance = accountBalance;
    }

    public BigDecimal getWithdrawBalance() {
        return withdrawBalance;
    }

    public void setWithdrawBalance(BigDecimal withdrawBalance) {
        this.withdrawBalance = withdrawBalance;
    }

    public BigDecimal getWithdrawFrozen() {
        return withdrawFrozen;
    }

    public void setWithdrawFrozen(BigDecimal withdrawFrozen) {
        this.withdrawFrozen = withdrawFrozen;
    }
}
