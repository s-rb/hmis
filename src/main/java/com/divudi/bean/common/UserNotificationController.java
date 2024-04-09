/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.pharmacy.PharmacySaleBhtController;
import com.divudi.data.BillTypeAtomic;
import static com.divudi.data.BillTypeAtomic.PHARMACY_ORDER;
import static com.divudi.data.BillTypeAtomic.PHARMACY_TRANSFER_REQUEST;
import com.divudi.data.MessageType;
import com.divudi.data.SmsSentResponse;
import com.divudi.data.TriggerType;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.entity.Bill;
import com.divudi.entity.UserNotification;
import com.divudi.entity.Institution;
import com.divudi.entity.Notification;
import com.divudi.entity.Sms;
import com.divudi.entity.WebUser;
import com.divudi.facade.NotificationFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.facade.UserNotificationFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class UserNotificationController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    TriggerSubscriptionController triggerSubscriptionController;
    @Inject
    SmsController smsController;
    @EJB
    private UserNotificationFacade ejbFacade;
    @EJB
    NotificationFacade notificationFacade;
    @EJB
    SmsFacade smsFacade;
    private UserNotification current;
    private List<UserNotification> items = null;

    @Inject
    PharmacySaleBhtController pharmacySaleBhtController;
    @Inject 
    SmsManagerEjb smsManager;

    public String navigateToRecivedNotification() {
        return "/Notification/user_notifications";
    }
    
    public String navigateToSentNotification() {
        return "/Notification/sent_notifications";
    }

    public void save(UserNotification userNotification) {
        if (userNotification == null) {
            return;
        }
        if (userNotification.getId() != null) {
            getFacade().edit(userNotification);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            userNotification.setCreatedAt(new Date());
            userNotification.setCreater(getSessionController().getLoggedUser());
            getFacade().create(userNotification);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public void prepareAdd() {
        current = new UserNotification();
    }

    public void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void userNotificationRequestComplete() {
        if (current == null) {
            JsfUtil.addErrorMessage("User Notification Error !");
            return;
        }
        current.getNotification().setCompleted(true);
        notificationFacade.edit(current.getNotification());
        current.setSeen(true);
        getFacade().edit(current);
        fillLoggedUserNotifications();
    }

    public void removeUserNotification(UserNotification un) {
        System.out.println("items = " + items.size());
        un.setSeen(true);
        getFacade().edit(un);
        fillLoggedUserNotifications();
    }

    private UserNotificationFacade getEjbFacade() {
        return ejbFacade;
    }

    public List<UserNotification> fillLoggedUserNotifications() {
        String jpql = "select un "
                + " from UserNotification un "
                + " where un.seen=:seen "
                + " and un.webUser=:wu "
                + " and un.notification.completed=:com";
        Map m = new HashMap();
        m.put("seen", false);
        m.put("com", false);
        m.put("wu", sessionController.getLoggedUser());
        items = getFacade().findByJpql(jpql, m);
        return items;
    }

    public String navigateToCurrentNotificationRequest(UserNotification cu) {
        if (cu.getNotification().getBill() == null) {
            return "";
        }
        Bill bill = cu.getNotification().getBill();
        BillTypeAtomic type = bill.getBillTypeAtomic();
        switch (type) {
            case PHARMACY_ORDER:
                pharmacySaleBhtController.generateIssueBillComponentsForBhtRequest(bill);
                return "/ward/ward_pharmacy_bht_issue";

            case PHARMACY_TRANSFER_REQUEST:
                pharmacySaleBhtController.generateIssueBillComponentsForBhtRequest(bill);
                return "/ward/ward_pharmacy_bht_issue";

            default:
                return "";

        }
    }

    public void createUserNotifications(Notification notification) {
        if (notification == null) {
            return;
        }
        if (notification.getBill() == null) {
            return;
        }
        if (notification.getBill().getBillTypeAtomic() == null) {
            return;
        }
        createUserNotificationsForMedium(notification);
    }

    private void createUserNotificationsForMedium(Notification n) {
        List<WebUser> notificationUsers = triggerSubscriptionController.fillWebUsers(n.getTriggerType());
        switch (n.getTriggerType().getMedium()) {
            case EMAIL:
                for (WebUser u : notificationUsers) {
                    String number = u.getWebUserPerson().getMobile();
                    System.out.println("number = " + number);
                    //TODo
                }
                break;
            case SMS:
                for (WebUser u : notificationUsers) {
                    String number = u.getWebUserPerson().getMobile();
                    sendSmsForUserSubscriptions(number);
                }
                break; 
            case SYSTEM_NOTIFICATION:
                for (WebUser u : notificationUsers) {
                    UserNotification nun = new UserNotification();
                    nun.setNotification(n);
                    nun.setWebUser(u);
                    getFacade().create(nun);
                }
                break;
        }

    }
    
    public void sendSmsForUserSubscriptions(String userMobNumber){
        Sms e = new Sms();
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setReceipientNumber(userMobNumber);
            e.setSendingMessage(createSmsForUserNotification());
            e.setDepartment(getSessionController().getLoggedUser().getDepartment());
            e.setInstitution(getSessionController().getLoggedUser().getInstitution());
            e.setPending(false);
            //e.setSmsType(MessageType.ChannelDoctorArrival);
            smsFacade.create(e);
            SmsSentResponse sent = smsManager.sendSmsByApplicationPreference(e.getReceipientNumber(), e.getSendingMessage(), sessionController.getApplicationPreference());
            e.setSentSuccessfully(sent.isSentSuccefully());
            e.setReceivedMessage(sent.getReceivedMessage());
            smsFacade.edit(e);
    }
    
    public String createSmsForUserNotification(){
//        s = template.replace("{patient_name}", patientName)
//                .replace("{doctor}", doc)
//                .replace("{appointment_time}", sessionTime)
//                .replace("{appointment_date}", sessionDate)
//                .replace("{serial_no}", String.valueOf(no))
//                .replace("{doc}", doc)
//                .replace("{time}", sessionTime)
//                .replace("{date}", sessionDate)
//                .replace("{No}", String.valueOf(no));
        
        return "";
    }

    public void createAllertMessage(Notification n) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage("Successful", "Your message: " + n.getMessage()));
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public UserNotificationController() {
    }

    public UserNotification getCurrent() {
        if (current == null) {
            current = new UserNotification();
        }
        return current;
    }

    public void setCurrent(UserNotification current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private UserNotificationFacade getFacade() {
        return ejbFacade;
    }

    public List<UserNotification> getItems() {
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = UserNotification.class)
    public static class UserNotificationConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            UserNotificationController controller = (UserNotificationController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "userNotificationController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof UserNotification) {
                UserNotification o = (UserNotification) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + UserNotification.class.getName());
            }
        }
    }

}
