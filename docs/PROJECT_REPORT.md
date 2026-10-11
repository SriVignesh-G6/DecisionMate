# DecisionMate: Smart Decision Making

## Abstract
DecisionMate is a web-based decision support application that helps users compare alternatives against multiple criteria. Users create a decision, define criteria, assign importance weights, list alternatives, and score each alternative on a 1–10 scale. The system applies a weighted scoring method, calculates normalized totals, and ranks the alternatives. The application uses React.js for the user interface, Java Spring Boot for REST APIs and business logic, and MySQL for persistent storage.

## Problem statement
When several alternatives have different strengths, decisions can become subjective and difficult to compare. DecisionMate provides a consistent process for recording relevant criteria and making trade-offs explicit.

## Objectives
- Create and manage decisions.
- Define decision criteria and importance weights.
- Compare multiple alternatives using consistent 1–10 scores.
- Calculate weighted scores and rank alternatives.
- Store decision data in a relational database.
- Present results in an understandable interface.

## Scope
The student-project version supports single-workspace decision management and benefit criteria where higher scores are better. It does not include user accounts, team collaboration, or cost-criterion inversion.

## Technology stack
- Frontend: React.js, Vite, JavaScript, CSS
- Backend: Java 17, Spring Boot, Spring Web, Spring Data JPA, Bean Validation
- Database: MySQL 8
- Communication: JSON over REST API

## Methodology
For criterion `j`, let its importance weight be `w_j`. Let `s_ij` be the score for alternative `i` against criterion `j`. The weighted score is:

`Score(i) = Σ(w_j × s_ij) / Σ(w_j)`

Weights are integers from 1 to 10, and scores are integers from 1 to 10. The result is normalized to a 1–10 scale. Alternatives are sorted by descending score.

## Main modules
1. Decision management
2. Criteria and weight management
3. Alternative management
4. Score matrix
5. Weighted result calculation
6. REST API and persistence

## Database entities
- Decision: title, description, created timestamp
- Criterion: name, weight, parent decision
- Alternative: name, description, parent decision
- Score: alternative, criterion, numeric score; one score per alternative/criterion pair

## Testing checklist
- Create a decision with a valid title.
- Verify empty titles are rejected.
- Add criteria with weights from 1 to 10.
- Add at least two alternatives.
- Save scores at the lower and upper bounds (1 and 10).
- Verify a score outside 1–10 is rejected.
- Verify result ranking changes when scores or weights change.
- Delete a decision and verify related records are removed.
- Verify the API returns a useful error when an ID does not exist.

## Limitations and future enhancements
- Add authentication and per-user data ownership.
- Support cost criteria where lower values are better.
- Add editing for existing criteria and alternatives.
- Add export to PDF/CSV and charts.
- Add unit/integration tests and database migrations.
- Deploy frontend and backend with environment-specific configuration.

## Conclusion
DecisionMate provides a structured, explainable approach to multi-criteria decisions. It makes the criteria, weights, scores, and final ranking visible so users can understand why an alternative ranks higher.
