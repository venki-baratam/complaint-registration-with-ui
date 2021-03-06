package org.complaint.persistence.complaint.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.complaint.domain.service.CrnGeneratorService;
import org.complaint.persistence.common.service.BoundaryService;
import org.complaint.persistence.common.service.DepartmentService;
import org.complaint.persistence.common.service.EmployeeService;
import org.complaint.persistence.complaint.entity.Complaint;
import org.complaint.persistence.complaint.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ComplaintService {

	@Autowired
	private CrnGeneratorService crnGeneratorService;

	@Autowired
	private ComplaintRepository complaintRepository;

	@Autowired
	private ComplaintTypeService complaintTypeService;

	@Autowired
	private ComplaintStatusService complaintStatusService;

	@Autowired
	private EmployeeService employeeService;

	@Autowired
	private BoundaryService boundaryService;

	@Autowired
	private DepartmentService departmentService;

	@PersistenceContext
	private EntityManager entityManager;

	public List<Complaint> getAll() {

		return complaintRepository.findAll();
	}

	public Optional<Complaint> getById(Long id) {

		return complaintRepository.findById(id);
	}

	@Transactional
	public Complaint createComplaint(final Complaint complaint) {

		populateCrn(complaint);

		populateComplaintType(complaint);

		populateComplaintStatus(complaint);

		populateLocation(complaint);

		populateWorkflowUser(complaint);

		populateDepartment(complaint);

		setAuditableFields(complaint);

		complaintRepository.save(complaint);

		return complaint;
	}

	@Transactional
	public Complaint updateComplaint(Complaint complaint) {

		Complaint newComplaint = complaintRepository.getOne(complaint.getId());

		newComplaint.setComments(complaint.getComments());

		updateComplaintStatus(newComplaint, complaint.getStatus().getName());

		// updateWorkflowUser(newComplaint, complaint.getAssignee().getId());

		setAuditableFields(newComplaint);

		complaintRepository.save(newComplaint);

		return newComplaint;
	}

	private void updateWorkflowUser(Complaint complaint, Long assignee) {
		if (assignee != null && assignee != null)
			complaint.setAssignee(employeeService.getById(assignee).get());

	}

	private void updateComplaintStatus(Complaint complaint, String name) {
		if (name != null && !name.isEmpty())
			complaint.setStatus(complaintStatusService.getByName(name));
	}

	private void populateDepartment(Complaint complaint) {
		if (complaint.getDepartment() != null && complaint.getDepartment().getCode() != null
				&& !complaint.getDepartment().getCode().isEmpty())
			complaint.setDepartment(departmentService.getByCode(complaint.getDepartment().getCode()));

	}

	private void populateWorkflowUser(Complaint complaint) {
		if (complaint.getAssignee() != null && complaint.getAssignee().getId() != null)
			complaint.setAssignee(employeeService.getById(complaint.getAssignee().getId()).get());

	}

	private void populateLocation(Complaint complaint) {
		if (complaint.getLatitude() != null && complaint.getLongitude() != null)
			complaint.setLocation(
					boundaryService.getByLongitudeAndLatitude(complaint.getLongitude(), complaint.getLatitude()));

	}

	private void populateComplaintStatus(Complaint complaint) {
		if (complaint.getStatus() != null && complaint.getStatus().getName() != null
				&& !complaint.getStatus().getName().isEmpty())
			complaint.setStatus(complaintStatusService.getByName(complaint.getStatus().getName()));

	}

	private void populateComplaintType(Complaint complaint) {

		if (complaint.getComplaintType() != null && complaint.getComplaintType().getCode() != null
				&& !complaint.getComplaintType().getCode().isEmpty())
			complaint.setComplaintType(complaintTypeService.getByCode(complaint.getComplaintType().getCode()));

	}

	private void populateCrn(Complaint complaint) {
		complaint.setCrn(crnGeneratorService.getCRN());
	}

	private void setAuditableFields(final Complaint complaint) {
		if (complaint.getCreatedBy() == null)
			complaint.setCreatedBy(1L);
		if (complaint.getCreatedDate() == null)
			complaint.setCreatedDate(new Date());
		complaint.setLastModifiedDate(new Date());
		complaint.setLastModifiedBy(1L);
	}

	public List<Complaint> search(final Complaint complaint) {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Complaint> createQuery = cb.createQuery(Complaint.class);
		final Root<Complaint> complaints = createQuery.from(Complaint.class);
		createQuery.select(complaints);
		final Metamodel m = entityManager.getMetamodel();
		final EntityType<Complaint> Complaint_ = m.entity(Complaint.class);

		final List<Predicate> predicates = new ArrayList<Predicate>();
		if (complaint.getCrn() != null) {
			final String crn = "%" + complaint.getCrn().toLowerCase() + "%";
			predicates.add(cb.isNotNull(complaints.get("crn")));
			predicates.add(cb
					.like(cb.lower(complaints.get(Complaint_.getDeclaredSingularAttribute("crn", String.class))), crn));
		}
		if (complaint.getComplaintType() != null && complaint.getComplaintType().getName() != null) {
			final String complaintType = "%" + complaint.getComplaintType().getName().toLowerCase() + "%";
			predicates.add(cb.isNotNull(complaints.get("complaintType.name")));
			predicates.add(cb.like(
					cb.lower(complaints
							.get(Complaint_.getDeclaredSingularAttribute("complaintType.name", String.class))),
					complaintType));
		}

		if (complaint.getDepartment() != null && complaint.getDepartment().getName() != null) {
			final String department = "%" + complaint.getDepartment().getName().toLowerCase() + "%";
			predicates.add(cb.isNotNull(complaints.get("department.name")));
			predicates
					.add(cb.like(
							cb.lower(complaints
									.get(Complaint_.getDeclaredSingularAttribute("department.name", String.class))),
							department));
		}

		if (complaint.getStatus() != null) {
			final String status = "%" + complaint.getStatus().getName().toLowerCase() + "%";
			predicates.add(cb.isNotNull(complaints.get("status")));
			predicates.add(cb.like(
					cb.lower(complaints.get(Complaint_.getDeclaredSingularAttribute("status", String.class))), status));
		}

		createQuery.where(predicates.toArray(new Predicate[] {}));
		final TypedQuery<Complaint> query = entityManager.createQuery(createQuery);
		return query.getResultList();

	}

}
