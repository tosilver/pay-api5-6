package co.b4pay.api.model;

import co.b4pay.api.model.base.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * 二维码信息
 *
 * @author YK
 * @version $Id: Channel.java, v 0.1 2018年4月21日 下午22:09:09 YK Exp $
 */
@Entity
@Table(name = "dst_frozen_capital_trade")
public class FrozenCapitalTrade extends BaseEntity {



    @Id
    private Long id;                                        // 唯一标识ID
    private Long frozenCapitalPoolId;                              //冻结资金池id
    private String outTradeNo;                                    // 客户订单号
    private Integer status;                                        //状态 :-1出金 0入金
    private BigDecimal money;                                       //金额
    private int FrozenCapitalStatus;                         //资金状态 0失败,1成功


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFrozenCapitalPoolId() {
        return frozenCapitalPoolId;
    }

    public void setFrozenCapitalPoolId(Long frozenCapitalPoolId) {
        this.frozenCapitalPoolId = frozenCapitalPoolId;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public int getFrozenCapitalStatus() {
        return FrozenCapitalStatus;
    }

    public void setFrozenCapitalStatus(int frozenCapitalStatus) {
        FrozenCapitalStatus = frozenCapitalStatus;
    }
}
