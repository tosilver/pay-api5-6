package co.b4pay.api.model;

import co.b4pay.api.model.base.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商城地址信息
 *
 * @author YK
 * @version $Id: Channel.java, v 0.1 2018年4月21日 下午22:09:09 YK Exp $
 */
@Entity
@Table(name = "dst_KJ_agreeapply")
public class KJAgreeapply extends BaseEntity {



    @Id
    private Long id;                                                // 唯一标识ID
    private String meruserId;                                        // 用户uid
    private String acctType;                                        // 卡类型
    private String acctNo;                                        // 银行卡号
    private String idNo;                                              // 证件号
    private String acctName;                                        // 户名
    private String mobile;                                        // 手机
    private String validdate;                                        // 有效期
    private String cvv2;                                        // cvv2
    private int status;                                        // 状态 0:已申请签约 1:签约成功
    private Date createTime;
    private Date updateTime;
    @Column(name = "response", columnDefinition = "longtext")
    private String response;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMeruserId() {
        return meruserId;
    }

    public void setMeruserId(String meruserId) {
        this.meruserId = meruserId;
    }

    public String getAcctType() {
        return acctType;
    }

    public void setAcctType(String acctType) {
        this.acctType = acctType;
    }

    public String getAcctNo() {
        return acctNo;
    }

    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }

    public String getIdNo() {
        return idNo;
    }

    public void setIdNo(String idNo) {
        this.idNo = idNo;
    }

    public String getAcctName() {
        return acctName;
    }

    public void setAcctName(String acctName) {
        this.acctName = acctName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getValiddate() {
        return validdate;
    }

    public void setValiddate(String validdate) {
        this.validdate = validdate;
    }

    public String getCvv2() {
        return cvv2;
    }

    public void setCvv2(String cvv2) {
        this.cvv2 = cvv2;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
