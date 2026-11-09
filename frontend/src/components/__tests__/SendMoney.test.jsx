import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import SendMoney from '../../pages/SendMoney';
import apiClient from '../../api/axios';

// Mock the API client
vi.mock('../../api/axios', () => {
  return {
    default: {
      post: vi.fn(),
      get: vi.fn(),
    },
  };
});

describe('SendMoney Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderComponent = () => {
    return render(
      <BrowserRouter>
        <SendMoney />
      </BrowserRouter>
    );
  };

  it('renders all form inputs and submit button', () => {
    renderComponent();

    expect(screen.getByLabelText(/recipient email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/amount in rupees/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/reference/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send money/i })).toBeInTheDocument();
  });

  it('validates that non-positive amount is rejected before calling API', async () => {
    renderComponent();
    const user = userEvent.setup();

    const emailInput = screen.getByLabelText(/recipient email/i);
    const amountInput = screen.getByLabelText(/amount in rupees/i);
    const submitBtn = screen.getByRole('button', { name: /send money/i });

    await user.type(emailInput, 'bob@example.com');
    await user.type(amountInput, '0');
    await user.click(submitBtn);

    expect(apiClient.post).not.toHaveBeenCalled();
    expect(await screen.findByText(/transfer amount must be strictly greater than/i)).toBeInTheDocument();
  });

  it('disables submit button during active transfer to prevent double-submits', async () => {
    // Return a promise that does not resolve immediately
    let resolvePromise;
    apiClient.post.mockImplementation(() => new Promise((res) => { resolvePromise = res; }));

    renderComponent();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/recipient email/i), 'bob@example.com');
    await user.type(screen.getByLabelText(/amount in rupees/i), '50.00');
    
    const submitBtn = screen.getByRole('button', { name: /send money/i });
    await user.click(submitBtn);

    // Button should now be disabled and text should change
    expect(submitBtn).toBeDisabled();
    expect(screen.getByText(/processing transfer.../i)).toBeInTheDocument();

    // Resolve the promise
    resolvePromise({ data: { amount: 5000, status: 'COMPLETED' } });
    await waitFor(() => {
      expect(screen.getByText(/transfer of ₹50.00 to bob@example.com completed successfully/i)).toBeInTheDocument();
    });
  });

  it('reuses the EXACT same Idempotency-Key when retrying after network failure', async () => {
    // 1st attempt: simulate network failure (no response object)
    apiClient.post.mockRejectedValueOnce(new Error('Network Error'));

    renderComponent();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/recipient email/i), 'carol@example.com');
    await user.type(screen.getByLabelText(/amount in rupees/i), '100.00');
    await user.type(screen.getByLabelText(/reference/i), 'Dinner');

    await user.click(screen.getByRole('button', { name: /send money/i }));

    // Expect network error and retry button to appear
    expect(await screen.findByText(/network failure: unable to reach payments server/i)).toBeInTheDocument();
    const retryBtn = screen.getByRole('button', { name: /retry same transfer/i });
    expect(retryBtn).toBeInTheDocument();

    // Capture the idempotency key passed in attempt 1
    const firstCallConfig = apiClient.post.mock.calls[0];
    const firstIdemKey = firstCallConfig[2].headers['Idempotency-Key'];
    expect(firstIdemKey).toBeTruthy();

    // 2nd attempt: mock success on retry
    apiClient.post.mockResolvedValueOnce({
      data: { amount: 10000, status: 'COMPLETED' },
    });

    await user.click(retryBtn);

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledTimes(2);
    });

    // Check that second attempt reused the identical key
    const secondCallConfig = apiClient.post.mock.calls[1];
    const secondIdemKey = secondCallConfig[2].headers['Idempotency-Key'];

    expect(secondIdemKey).toBe(firstIdemKey);
    expect(await screen.findByText(/completed successfully/i)).toBeInTheDocument();
  });

  it('switches to Standing Orders tab and creates recurring auto-transfer', async () => {
    apiClient.get.mockResolvedValueOnce({ data: [] });
    apiClient.post.mockResolvedValueOnce({
      data: {
        id: 1,
        recipientEmail: 'landlord@example.com',
        amount: 250000,
        frequency: 'MONTHLY',
        status: 'ACTIVE',
      },
    });
    // For refresh after post
    apiClient.get.mockResolvedValueOnce({
      data: [
        {
          id: 1,
          recipientEmail: 'landlord@example.com',
          amount: 250000,
          frequency: 'MONTHLY',
          status: 'ACTIVE',
          nextExecutionAt: '2026-12-01T00:00:00Z',
        },
      ],
    });

    renderComponent();
    const user = userEvent.setup();

    // Click on Standing Orders tab
    const standingOrdersTab = screen.getByRole('button', { name: /standing orders/i });
    await user.click(standingOrdersTab);

    expect(screen.getByLabelText(/recipient email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/frequency/i)).toBeInTheDocument();

    await user.type(screen.getByLabelText(/recipient email/i), 'landlord@example.com');
    await user.type(screen.getByLabelText(/amount in rupees/i), '2500.00');
    await user.selectOptions(screen.getByLabelText(/frequency/i), 'MONTHLY');

    const scheduleBtn = screen.getByRole('button', { name: /schedule auto-transfer/i });
    await user.click(scheduleBtn);

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith(
        '/scheduled-transfers',
        expect.objectContaining({
          toEmail: 'landlord@example.com',
          amount: 250000,
          frequency: 'MONTHLY',
        })
      );
    });

    expect(await screen.findByText(/recurring monthly transfer of ₹2500.00 scheduled successfully/i)).toBeInTheDocument();
  });
});
