package co.b4pay.api.model;

import co.b4pay.api.model.base.BaseEntity;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 渠道信息
 *
 * @author YK
 * @version $Id: Channel.java, v 0.1 2018年4月21日 下午22:09:09 YK Exp $
 */
@Entity
@Table(name = "dst_channel")
@Proxy(lazy = false)
public class Channel extends BaseEntity {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -3050115515392663063L;

    /**
     * 实体编号（唯一标识）
     */
    @Id
    private Long id;                                        // 唯一标识ID
    private String name;                                      // 通道名
    private BigDecimal amountInit;                                // 限额初始额度（单位：万）
    private BigDecimal amountLimit;                               // 限额（单位：万），24小时内重置
    private Date resetTime;                                 // 额度重置时间
    private String testAppid;                                 // 测试环境APPID
    private String testPid;                                   // 测试环境商户PID
    private String prodPid;                                   // 生产环境商户PID
    private String testPrivateKey;                            // 测试环境私钥
    private String prodPrivateKey;                            // 生产环境私钥
    private String testPublicKey;                             // 测试环境公钥
    private String prodPublicKey;                             // 生产环境公钥
    private String prodAppid;                                 // 生产环境APPID
    private Integer status;
    private BigDecimal unitPrice;                                 //单笔限额
    private Date updateTime;                                 //更新时间
    private BigDecimal minPrice;                                   //单笔最低限额
    private String goodsTypeId;                                 //商品类别id
    private String ip4;                                         //ip4地址
    private Date lastFailTime;                                 //最后交易失败时间
    private Integer product;                                     //产品
    private BigDecimal amountMin;                               //最低额度
    private BigDecimal rate;                                        //单笔成单速率
    private Float fzPercentage;                                    //支付宝分账百分比
    private Date lastSuccessTime;                                 //最后交易成功时间
    @ManyToOne
    @JoinColumn(name = "router_id")
    private Router router;

    public Float getFzPercentage() {
        return fzPercentage;
    }

    public void setFzPercentage(Float fzPercentage) {
        this.fzPercentage = fzPercentage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmountInit() {
        return amountInit;
    }

    public void setAmountInit(BigDecimal amountInit) {
        this.amountInit = amountInit;
    }

    public BigDecimal getAmountLimit() {
        return amountLimit;
    }

    public void setAmountLimit(BigDecimal amountLimit) {
        this.amountLimit = amountLimit;
    }

    public Date getResetTime() {
        return resetTime;
    }

    public void setResetTime(Date resetTime) {
        this.resetTime = resetTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTestAppid() {
        return testAppid;
    }

    public void setTestAppid(String testAppid) {
        this.testAppid = testAppid;
    }

    public String getProdAppid() {
        return prodAppid;
    }

    public void setProdAppid(String prodAppid) {
        this.prodAppid = prodAppid;
    }

    public String getTestPid() {
        return testPid;
    }

    public void setTestPid(String testPid) {
        this.testPid = testPid;
    }

    public String getTestPrivateKey() {
        return testPrivateKey;
    }

    public void setTestPrivateKey(String testPrivateKey) {
        this.testPrivateKey = testPrivateKey;
    }

    public String getTestPublicKey() {
        return testPublicKey;
    }

    public void setTestPublicKey(String testPublicKey) {
        this.testPublicKey = testPublicKey;
    }

    public String getProdPid() {
        return prodPid;
    }

    public void setProdPid(String prodPid) {
        this.prodPid = prodPid;
    }

    public Router getRouter() {
        return router;
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    public String getProdPrivateKey() {
        return prodPrivateKey;
    }

    public void setProdPrivateKey(String prodPrivateKey) {
        this.prodPrivateKey = prodPrivateKey;
    }

    public String getProdPublicKey() {
        return prodPublicKey;
    }

    public void setProdPublicKey(String prodPublicKey) {
        this.prodPublicKey = prodPublicKey;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }


    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public String getGoodsTypeId() {
        return goodsTypeId;
    }

    public void setGoodsTypeId(String goodsTypeId) {
        this.goodsTypeId = goodsTypeId;
    }

    public String getIp4() {
        return ip4;
    }

    public void setIp4(String ip4) {
        this.ip4 = ip4;
    }

    public Date getLastFailTime() {
        return lastFailTime;
    }

    public void setLastFailTime(Date lastFailTime) {
        this.lastFailTime = lastFailTime;
    }

    public Integer getProduct() {
        return product;
    }

    public void setProduct(Integer product) {
        this.product = product;
    }

    public BigDecimal getAmountMin() {
        return amountMin;
    }

    public void setAmountMin(BigDecimal amountMin) {
        this.amountMin = amountMin;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public Date getLastSuccessTime() {
        return lastSuccessTime;
    }

    public void setLastSuccessTime(Date lastSuccessTime) {
        this.lastSuccessTime = lastSuccessTime;
    }


    @Override
    public String toString() {
        return "Channel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", amountInit=" + amountInit +
                ", amountLimit=" + amountLimit +
                ", resetTime=" + resetTime +
                ", testAppid='" + testAppid + '\'' +
                ", testPid='" + testPid + '\'' +
                ", prodPid='" + prodPid + '\'' +
                ", testPrivateKey='" + testPrivateKey + '\'' +
                ", prodPrivateKey='" + prodPrivateKey + '\'' +
                ", testPublicKey='" + testPublicKey + '\'' +
                ", prodPublicKey='" + prodPublicKey + '\'' +
                ", prodAppid='" + prodAppid + '\'' +
                ", status=" + status +
                ", unitPrice=" + unitPrice +
                ", updateTime=" + updateTime +
                ", minPrice=" + minPrice +
                ", goodsTypeId='" + goodsTypeId + '\'' +
                ", ip4='" + ip4 + '\'' +
                ", lastFailTime=" + lastFailTime +
                ", product=" + product +
                ", amountMin=" + amountMin +
                ", rate=" + rate +
                ", fzPercentage=" + fzPercentage +
                ", lastSuccessTime=" + lastSuccessTime +
                ", router=" + router +
                '}';
    }
}
