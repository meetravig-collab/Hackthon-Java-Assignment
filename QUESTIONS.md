# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly. 

What are your thoughts on the pros and cons of each approach? Which would you choose and why?

**Answer:**
```Here  are pros :::

Parallel Development: Frontend and backend teams can work simultaneously. The YAML acts as a "mock" so the frontend doesn't have to wait for the backend logic to be finished.

Consistency and Quality: By defining schemas (like your Warehouse schema) upfront, you ensure data structures are reused and validated strictly.

Automatic Documentation: You get Swagger/Redoc UI for free, which is always in sync with the actual API contract.

Client SDK Generation: You can generate client libraries in multiple languages (TypeScript, Python, etc.) automatically.

Here are cons:::

Overhead: It requires proficiency in OpenAPI syntax and adds an extra step to the development workflow.

Abstraction Leakage: Sometimes the generated code is "clunky" or doesn't follow your project’s specific architectural patterns without heavy customization.

---

## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project? 

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
Integration Bottlenecks: Focusing heavily on integration and parameterized tests (which are slower to run than simple unit tests) can slow down your CI/CD pipeline. Developers might stop running tests locally if the suite takes 10+ minutes to finish.

False Sense of Security: High coverage in "Happy Path" E2E tests doesn't mean the system is robust. If you skip negative testing (testing for failures) to save time, the system might crash gracefully in staging but fail catastrophically in production when it hits an edge case you "deprioritized."

Maintenance Burden: Parameterized tests are great until the business logic changes. One change to a Warehouse capacity rule could break 50 different test cases simultaneously, leading to "test fatigue" where developers start ignoring failures.

```
