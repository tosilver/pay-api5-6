package co.b4pay.api.model;


import co.b4pay.api.model.base.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

/****
 * 二维码管理:因为谷歌帮助类已经有QRCode 为不影响命名,所以使用小写命名
 * @author Tosdom
 */
@Entity
@Table(name = "dst_qrcode")
public class qrcode extends BaseEntity {

    @Id
    private Long id;
    private Long merchantId;          //所属商户id
    private String Name;                   //二维码名称
    private BigDecimal money;               //二维码金额
    private String codePath;                //二维码存放地址
    private String codeData;                //二维码数据
    private int status;                      //状态
    private int codeType;                //二维码类型 0支付宝,1微信,2聚合码
    private Date  lastRequestTime;           //最后请求时间
    private BigDecimal turnover;            //日收入
    private BigDecimal rate;                     //速率
    private int no;//二维码记录


    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getCodePath() {
        return codePath;
    }

    public void setCodePath(String codePath) {
        this.codePath = codePath;
    }

    public int getCodeType() {
        return codeType;
    }

    public void setCodeType(int codeType) {
        this.codeType = codeType;
    }

    public String getCodeData() {
        return codeData;
    }

    public void setCodeData(String codeData) {
        this.codeData = codeData;
    }

    public Date getLastRequestTime() {
        return lastRequestTime;
    }

    public void setLastRequestTime(Date lastRequestTime) {
        this.lastRequestTime = lastRequestTime;
    }

    public BigDecimal getTurnover() {
        return turnover;
    }

    public void setTurnover(BigDecimal turnover) {
        this.turnover = turnover;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }
}
