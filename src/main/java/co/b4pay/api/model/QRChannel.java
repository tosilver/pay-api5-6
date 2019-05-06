package co.b4pay.api.model;

import co.b4pay.api.model.base.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 二维码信息
 *
 * @author YK
 * @version $Id: Channel.java, v 0.1 2018年4月21日 下午22:09:09 YK Exp $
 */
@Entity
@Table(name = "dst_qr_channel")
public class QRChannel extends BaseEntity {



    @Id
    private Long id;                                        // 唯一标识ID
    private Long merchantId;                              //所属商户ip
    private String Name;                                    // 通道名称
    private Integer status;
    private BigDecimal turnover;                               //营业额(每日清空)
    private BigDecimal rechargeAmount;                         //充值金额(资金池)
    private BigDecimal FrozenCapitalPool;                         //冻结资金池(已请求未支付)
    private Date lastRequestTime;                              //最后请求时间
    private BigDecimal rate;                                    //请求速率




    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public BigDecimal getTurnover() {
        return turnover;
    }

    public void setTurnover(BigDecimal turnover) {
        this.turnover = turnover;
    }


    public BigDecimal getRechargeAmount() {
        return rechargeAmount;
    }

    public void setRechargeAmount(BigDecimal rechargeAmount) {
        this.rechargeAmount = rechargeAmount;
    }

    public Date getLastRequestTime() {
        return lastRequestTime;
    }

    public void setLastRequestTime(Date lastRequestTime) {
        this.lastRequestTime = lastRequestTime;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getFrozenCapitalPool() {
        return FrozenCapitalPool;
    }

    public void setFrozenCapitalPool(BigDecimal frozenCapitalPool) {
        FrozenCapitalPool = frozenCapitalPool;
    }

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
}
