package co.b4pay.api.model;

import co.b4pay.api.model.base.BaseEntity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "dst_trade")
@EntityListeners(AuditingEntityListener.class)
public class Trade extends BaseEntity {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -3050115514689954631L;

    /**
     * 实体编号（唯一标识）
     */
    @Id
    @NotBlank
    private String id;                          // 唯一标识ID
    private Long merchantId;                    // 商户ID
    private Long channelId;                     // 通道ID
    private Long addressId;                     // 地址ID
    private Long qrchannelId;                     // 码商通道ID
    private Long qrcodeId;                     // 二维码ID
    private Long time;                          // 耗时
    private BigDecimal totalAmount;             // 总金额
    private BigDecimal requestAmount;
    private String notifyUrl;                   // 商户回调地址
    private String merchantOrderNo;             // 商户订单号
    private String payOrderNo;                  // 上游订单号
    private BigDecimal costRate;                // 成本费率
    private BigDecimal payCost;                 // 代付成本
    private BigDecimal serviceCharge;           // 手续费
    private BigDecimal accountAmount;           // 到账金额
    private Integer tradeState;                 // 交易状态
    private BigDecimal fzAmount;                //支付宝分账金额
    private Integer fzStatus;                   // 支付宝分账状态
    private Long transinId;                     //分账账号Id
    private String remark;
    private String paymentTime;                 //支付时间

    @Column(name = "header", columnDefinition = "text")
    private String header;
    @Column(name = "request", columnDefinition = "mediumtext")
    private String request;
    @Column(name = "response", columnDefinition = "longtext")
    private String response;

    private Integer status;
    @CreatedDate
    private Date createTime;
    @LastModifiedDate
    private Date updateTime;

    public BigDecimal getFzAmount() {
        return fzAmount;
    }

    public void setFzAmount(BigDecimal fzAmount) {
        this.fzAmount = fzAmount;
    }

    public Long getTransinId() {
        return transinId;
    }

    public void setTransinId(Long transinId) {
        this.transinId = transinId;
    }

    public Integer getFzStatus() {
        return fzStatus;
    }

    public void setFzStatus(Integer fzStatus) {
        this.fzStatus = fzStatus;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getMerchantOrderNo() {
        return merchantOrderNo;
    }

    public void setMerchantOrderNo(String merchantOrderNo) {
        this.merchantOrderNo = merchantOrderNo;
    }


    public BigDecimal getPayCost() {
        return payCost;
    }

    public void setPayCost(BigDecimal payCost) {
        this.payCost = payCost;
    }

    public BigDecimal getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(BigDecimal serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public BigDecimal getAccountAmount() {
        return accountAmount;
    }

    public void setAccountAmount(BigDecimal accountAmount) {
        this.accountAmount = accountAmount;
    }

    public Integer getTradeState() {
        return tradeState;
    }

    public void setTradeState(Integer tradeState) {
        this.tradeState = tradeState;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public void setCostRate(BigDecimal costRate) {
        this.costRate = costRate;
    }

    public BigDecimal getCostRate() {
        return costRate;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getPayOrderNo() {
        return payOrderNo;
    }

    public void setPayOrderNo(String payOrderNo) {
        this.payOrderNo = payOrderNo;
    }

    public String getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(String paymentTime) {
        this.paymentTime = paymentTime;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public BigDecimal getRequestAmount() {
        return requestAmount;
    }

    public void setRequestAmount(BigDecimal requestAmount) {
        this.requestAmount = requestAmount;
    }

    public Long getQrchannelId() {
        return qrchannelId;
    }

    public void setQrchannelId(Long qrchannelId) {
        this.qrchannelId = qrchannelId;
    }

    public Long getQrcodeId() {
        return qrcodeId;
    }

    public void setQrcodeId(Long qrcodeId) {
        this.qrcodeId = qrcodeId;
    }
}
