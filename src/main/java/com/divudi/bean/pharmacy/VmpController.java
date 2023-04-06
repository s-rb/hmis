/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.entity.Category;
import com.divudi.entity.pharmacy.MeasurementUnit;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vtm;
import com.divudi.entity.pharmacy.VirtualProductIngredient;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.VmpFacade;
import com.divudi.facade.VtmsVmpsFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
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
public class VmpController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private VmpFacade ejbFacade;
    @EJB
    private SpecialityFacade specialityFacade;
    @Inject
    private BillBeanController billBean;
    List<Vmp> selectedItems;
    private Vmp current;
    private List<Vmp> items = null;
    String selectText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;
    VirtualProductIngredient addingVtmInVmp;
    VirtualProductIngredient removingVtmInVmp;
    @Inject
    VtmInVmpController vtmInVmpController;
    @EJB
    VtmsVmpsFacade vivFacade;
    List<VirtualProductIngredient> vivs;

    List<Vmp> vmpList;

    public String navigateToListAllVmps() {
        String jpql = "Select vmp "
                + " from Vmp vmp "
                + " where vmp.retired=:ret "
                + " order by vmp.name";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findBySQL(jpql, m);
        return "/emr/reports/vmps?faces-redirect=true";
    }

    public List<Vmp> completeVmp(String query) {

        String sql;
        if (query == null) {
            vmpList = new ArrayList<Vmp>();
        } else {
            sql = "select c from Vmp c where c.retired=false and upper(c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //////// // System.out.println(sql);
            vmpList = getFacade().findBySQL(sql);
        }
        return vmpList;
    }

    public Vmp findVmpByName(String name) {
        String jpql;
        if (name == null) {
            return null;
        }
        jpql = "select c "
                + " from Vmp c "
                + " where c.retired=:ret "
                + " and c.name=:name";
        Map m = new HashMap();
        m.put("name", name);
        m.put("ret", false);
        return getFacade().findFirstByJpql(jpql, m);
    }

    public Vmp createVmp(String vmpName, 
            Vtm vtm,
            Category dosageForm,
            Double strengthOfAnIssueUnit, 
            MeasurementUnit strengthUnit, 
            Double issueUnitsPerPack,
            MeasurementUnit packUnit,
            Double minimumIssueQuantity,
            MeasurementUnit minimumIssueQuantityUnit,
            Double issueMultipliesQuantity,
            MeasurementUnit issueMultipliesQuantityUnit) {
        Vmp v;
        v = findVmpByName(vmpName);
        if (v != null) {
            return v;
        }
        v = new Vmp();
        v.setName(vmpName);
        v.setCode("vmp_" + CommonController.nameToCode(vmpName));
        v.setVtm(vtm);
        v.setDosageForm(dosageForm);
        v.setStrengthOfAnIssueUnit(strengthOfAnIssueUnit);
        v.setStrengthUnit(strengthUnit);
        v.setPackUnit(packUnit);
        v.setIssueUnitsPerPackUnit(issueUnitsPerPack);
        v.setMinimumIssueQuantity(minimumIssueQuantity);
        v.setMinimumIssueQuantityUnit(minimumIssueQuantityUnit);
        v.setIssueMultipliesQuantity(issueMultipliesQuantity);
        v.setIssueMultipliesUnit(issueMultipliesQuantityUnit);
        getFacade().create(v);
        return v;
}


    public List<VirtualProductIngredient> getVivs() {
        if (getCurrent().getId() == null) {
            return new ArrayList<VirtualProductIngredient>();
        } else {

            vivs = getVivFacade().findBySQL("select v from VtmsVmps v where v.vmp.id = " + getCurrent().getId());

            if (vivs == null) {
                return new ArrayList<VirtualProductIngredient>();
            }

            return vivs;
        }
    }

    public String getVivsAsString(Vmp vmp) {
        return getVivsAsString(getVivs(vmp));
    }

    public String getVivsAsString(List<VirtualProductIngredient> gs) {
        String str = "";
        for (VirtualProductIngredient g : gs) {
            if (g.getVtm() == null || g.getVtm().getName() == null) {
                continue;
            }
            if ("".equals(str)) {
                str = g.getVtm().getName();
            } else {
                str = str + ", " + g.getVtm().getName();
            }
        }
        return str;
    }

    public List<VirtualProductIngredient> getVivs(Vmp vmp) {
        List<VirtualProductIngredient> gs;
        if (vmp == null) {
            return new ArrayList<>();
        } else {
            String j = "select v from VtmsVmps v where v.vmp=:vmp";
            Map m = new HashMap();
            m.put("vmp", vmp);
            gs = getVivFacade().findBySQL(j, m);
            if (gs == null) {
                return new ArrayList<>();
            }
            return gs;
        }
    }

    public void remove() {
        getVivFacade().remove(removingVtmInVmp);
    }

    public void setVivs(List<VirtualProductIngredient> vivs) {
        this.vivs = vivs;
    }

    private boolean errorCheck() {
        if (addingVtmInVmp == null) {
            return true;
        }
        if (addingVtmInVmp.getVtm() == null) {
            UtilityController.addErrorMessage("Select Vtm");
            return true;
        }
//        TODO:Message
        if (current == null) {
            return true;
        }
        if (addingVtmInVmp.getStrength() == 0.0) {
            UtilityController.addErrorMessage("Type Strength");
            return true;
        }
        if (current.getCategory() == null) {
            UtilityController.addErrorMessage("Select Category");
            return true;
        }
        if (addingVtmInVmp.getStrengthUnit() == null) {
            UtilityController.addErrorMessage("Select Strenth Unit");
            return true;
        }

        return false;
    }

    public void addVtmInVmp() {
        if (errorCheck()) {
            return;
        }

        saveVmp();
        getAddingVtmInVmp().setVmp(current);
        getVivFacade().create(getAddingVtmInVmp());

        UtilityController.addSuccessMessage("Added");

        addingVtmInVmp = null;

    }

    private void saveVmp() {
        if (current.getName() == null || current.getName().equals("")) {
            current.setName(createVmpName());
        }

        if (current.getId() == null || current.getId() == 0) {
            getFacade().create(current);
        } else {
            getFacade().edit(current);
        }

    }

    public String createVmpName() {
        return addingVtmInVmp.getVtm().getName() + " " + addingVtmInVmp.getStrength() + " " + addingVtmInVmp.getStrengthUnit().getName() + " " + current.getCategory().getName();
    }

    public VirtualProductIngredient getAddingVtmInVmp() {
        if (addingVtmInVmp == null) {
            addingVtmInVmp = new VirtualProductIngredient();
        }
        return addingVtmInVmp;
    }

    public void setAddingVtmInVmp(VirtualProductIngredient addingVtmInVmp) {
        this.addingVtmInVmp = addingVtmInVmp;
    }

    public VirtualProductIngredient getRemovingVtmInVmp() {
        return removingVtmInVmp;
    }

    public void setRemovingVtmInVmp(VirtualProductIngredient removingVtmInVmp) {
        this.removingVtmInVmp = removingVtmInVmp;
    }

    public VtmsVmpsFacade getVivFacade() {
        return vivFacade;
    }

    public void setVivFacade(VtmsVmpsFacade vivFacade) {
        this.vivFacade = vivFacade;
    }

    public List<Vmp> completeInvest(String query) {
        List<Vmp> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<Vmp>();
        } else {
            sql = "select c from Vmp c where c.retired=false and upper(c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findBySQL(sql);
        }
        return suggestions;
    }

    public boolean isBilledAs() {
        return billedAs;
    }

    public void setBilledAs(boolean billedAs) {
        this.billedAs = billedAs;
    }

    public boolean isReportedAs() {
        return reportedAs;
    }

    public void setReportedAs(boolean reportedAs) {
        this.reportedAs = reportedAs;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public String getBulkText() {

        return bulkText;
    }

    public void setBulkText(String bulkText) {
        this.bulkText = bulkText;
    }

    public List<Vmp> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findBySQL("select c from Vmp c where c.retired=false order by c.name");
        } else {
            String sql = "select c from Vmp c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name";
            selectedItems = getFacade().findBySQL(sql);
        }
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Vmp();
        addingVtmInVmp = new VirtualProductIngredient();
    }

    public void bulkUpload() {
        List<String> lstLines = Arrays.asList(getBulkText().split("\\r?\\n"));
        for (String s : lstLines) {
            List<String> w = Arrays.asList(s.split(","));
            try {
                String code = w.get(0);
                String ix = w.get(1);
                String ic = w.get(2);
                String f = w.get(4);
                //////// // System.out.println(code + " " + ix + " " + ic + " " + f);

                Vmp tix = new Vmp();
                tix.setCode(code);
                tix.setName(ix);
                tix.setDepartment(null);

            } catch (Exception e) {
            }

        }
    }

    public void setSelectedItems(List<Vmp> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Updated Successfully.");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public VmpFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(VmpFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public VmpController() {
    }

    public Vmp getCurrent() {
        if (current == null) {
            current = new Vmp();
        }
        return current;
    }

    public void setCurrent(Vmp current) {
        this.current = current;
        if (current != null) {
            if (current.getBilledAs() == current) {
                billedAs = false;
            } else {
                billedAs = true;
            }
            if (current.getReportedAs() == current) {
                reportedAs = false;
            } else {
                reportedAs = true;
            }
        }
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);

            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private VmpFacade getFacade() {
        return ejbFacade;
    }

    public List<Vmp> getItems() {
        if (items == null) {
            String j;
            j = "select v "
                    + " from Vmp v "
                    + " where v.retired=false "
                    + " order by v.name";
            items = getFacade().findBySQL(j);
        }
        return items;
    }

    public SpecialityFacade getSpecialityFacade() {
        return specialityFacade;
    }

    public void setSpecialityFacade(SpecialityFacade specialityFacade) {
        this.specialityFacade = specialityFacade;
    }

    /**
     *
     */
    @FacesConverter("vmp")
    public static class VmpControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            VmpController controller = (VmpController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "vmpController");
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
            if (object instanceof Vmp) {
                Vmp o = (Vmp) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + VmpController.class.getName());
            }
        }
    }
}
