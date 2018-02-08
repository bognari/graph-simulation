// Q1:
MATCH
  (st)-[:teachingAssistantOf]-(course)
MATCH
  (prof)-[:teacherOf]-(course),
  (st)-[:advisor]-(prof)
OPTIONAL MATCH
  (st)-[:takesCourse]-(course2),
  (pub1)-[:publicationAuthor]-(st)
OPTIONAL MATCH
  (prof)-[:researchInterest]-(resint),
  (pub2)-[:publicationAuthor]-(prof)
RETURN st, course, prof, course2, pub1, resint, pub2;

// Q2:
MATCH
  (pub:Publication)-[:publicationAuthor]-(st),
  (pub)-[:publicationAuthor]-(prof)
MATCH
  (st)-[:undergraduateDegreeFrom]-(univ),
  (dept)-[:subOrganizationOf]-(univ)
MATCH
  (st)-[:memberOf]-(dept),
  (prof)-[:worksFor]-(dept)
OPTIONAL MATCH
  (head)-[:headOf]-(dept),
  (others)-[:worksFor]-(dept)
OPTIONAL MATCH
  (prof)-[:doctoralDegreeFrom]-(univ1),
  (prof)-[:researchInterest]-(resint1)
RETURN pub, st, st.emailAddress, st.telephone, prof, univ, dept, head, others, univ1, resint1;

// Q3:
MATCH
  (pub)-[:publicationAuthor]-(st:GraduateStudent),
  (pub)-[:publicationAuthor]-(prof:FullProfessor)
MATCH
  (st)-[:advisor]-(prof)
MATCH
  (st)-[:memberOf]-(dept),
  (prof)-[:worksFor]-(dept)
OPTIONAL MATCH
  (st)-[:undergraduateDegreeFrom]-(univ1)
OPTIONAL MATCH
  (prof)-[:doctoralDegreeFrom]-(univ),
  (prof)-[:researchInterest]-(resint)
OPTIONAL MATCH
  (head)-[:headOf]-(dept),
  (others)-[:worksFor]-(dept)
RETURN pub, st, prof, dept, univ1, univ, resint, head, others;

// Q4:
MATCH
  (x:FullProfessor)-[:worksFor]-({id: '<http://www.Department9.University9999.edu>'})
OPTIONAL MATCH
  (y)-[:advisor]-(x),
  (x)-[:teacherOf]-(x),
  (y)-[:takesCourse]-(z)
RETURN x, y, z;

// Q5:
MATCH
  (x:FullProfessor)-[:worksFor]-({id: '<http://www.Department0.University12.edu>'})
OPTIONAL MATCH
  (y)-[:advisor]-(x),
  (x)-[:teacherOf]-(x),
  (y)-[:takesCourse]-(z)
RETURN x, y, z;

// Q6:
MATCH
  (x:FullProfessor)-[:worksFor]-({id: '<http://www.Department0.University12.edu>'})
RETURN x, x.emailAddress, x.telephone, x.name;

