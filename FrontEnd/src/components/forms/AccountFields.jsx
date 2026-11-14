import { Field, PasswordField } from "../common/UI";
export function ProfileFields({ values, setValues }) {
  const change = (event) =>
    setValues((previous) => ({
      ...previous,
      [event.target.name]: event.target.value,
    }));
  return (
    <>
      <div className="form-grid">
        <Field
          label="First name"
          name="firstName"
          value={values.firstName || ""}
          onChange={change}
          required
          maxLength={100}
          autoComplete="given-name"
        />
        <Field
          label="Last name"
          name="lastName"
          value={values.lastName || ""}
          onChange={change}
          required
          maxLength={100}
          autoComplete="family-name"
        />
      </div>
      <Field
        label="Email address"
        name="email"
        type="email"
        value={values.email || ""}
        onChange={change}
        required
        maxLength={255}
        autoComplete="email"
      />
      <Field
        label="Phone number"
        name="phone"
        type="tel"
        value={values.phone || ""}
        onChange={change}
        maxLength={30}
        pattern="[+0-9 \(\)\-]{0,30}"
        autoComplete="tel"
        hint="Optional. Include your country code if needed."
      />
    </>
  );
}
export function RegistrationFields({ values, setValues }) {
  const change = (event) =>
    setValues((previous) => ({
      ...previous,
      [event.target.name]: event.target.value,
    }));
  return (
    <>
      <ProfileFields values={values} setValues={setValues} />
      <Field
        label="Username"
        name="username"
        value={values.username || ""}
        onChange={change}
        autoComplete="username"
        required
        minLength={3}
        maxLength={50}
        pattern="[A-Za-z0-9_.\-]{3,50}"
        hint="3–50 letters, numbers, dots, underscores or hyphens."
      />
      <PasswordField
        name="password"
        value={values.password || ""}
        onChange={change}
        minLength={8}
        maxLength={72}
        autoComplete="new-password"
      />
      <PasswordField
        label="Confirm password"
        name="confirmation"
        value={values.confirmation || ""}
        onChange={change}
        minLength={8}
        maxLength={72}
        autoComplete="new-password"
      />
    </>
  );
}
export const profilePayload = (values) => ({
  firstName: values.firstName.trim(),
  lastName: values.lastName.trim(),
  email: values.email.trim(),
  phone: values.phone || "",
});
